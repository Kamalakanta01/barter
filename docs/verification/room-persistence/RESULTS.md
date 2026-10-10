# Room persistence verification

**Result: PASS.** A listing removed from the app stays removed across a full process restart, and a
listing left behind is still there afterwards.

Run on 2026-10-08 against commit `d4ce306` ("Initial commit: Even barter app with Room storage"),
emulator `even36g` (Android 16 / API 36), debug build.

## The check

List two items, remove one, close the app completely, reopen it, and the remaining item must still
be there. This is the first time the app stores anything on disk, so it exercises
`RoomBarterRepository` end to end: write on every change, then read back from a cold start.

## Steps and evidence

| # | Step | Expected | Observed | Screenshot |
|---|------|----------|----------|------------|
| 1 | Fresh install (`pm clear`) | No listings | `Nothing to browse yet. Items listed by other people will show up here.` | `01-fresh-install-empty.png` |
| 2 | List "Mechanical keyboard" / wants "Plants" | Card appears | Present | `02-two-items-listed.png` |
| 3 | List "Road bike" / wants "Camera gear" | Card appears | Both cards present | `02-two-items-listed.png` |
| 4 | Remove "Road bike" | Removed, no dialog | Removed immediately — no confirmation, correctly, because it had no pending offers | `03-road-bike-removed.png` |
| 5 | Read the table on disk | Only the keyboard | `1\|Mechanical keyboard\|Plants\|0` | — |
| 6 | `am force-stop` | Process gone | pid 3528 gone; launcher resumed | — |
| 7 | Cold relaunch (pid 3967) | Keyboard still there | `Mechanical keyboard` / `Wants Plants` present; road bike absent | `04-after-cold-restart-persisted.png` |

Tab bar captured in all three states at the end of the run: `05-tab-browse.png`,
`05-tab-myitems.png`, `05-tab-offers.png`.

## Notes worth keeping

**The data was in the WAL, not the main file, when the app was killed.** At step 6 the on-disk files
were `barter.db` at 4096 bytes (header only) alongside a 61 KB `barter.db-wal`. SQLite recovered the
rows from the WAL on reopen, so a hard kill is not losing writes — but this is worth remembering
before anyone concludes a successful restart means the main database file was written. Reading the
table with the app stopped is what proves it, and it did:

```
adb shell "run-as dev.barter.app.debug sqlite3 databases/barter.db \
  'SELECT id,title,wants,isTraded FROM listings'"
1|Mechanical keyboard|Plants|0
```

**`force-stop` is a hard kill, not a clean shutdown.** It sends SIGKILL, so nothing got a chance to
checkpoint or flush. Passing under those conditions is the stronger version of this test.

**No demo data exists any more.** `Seed.kt` was removed when Room replaced the in-memory store, so a
fresh install starts completely empty. That is why the run above lists its own two items. A
side effect: with one trader and no seed, a pending offer can never exist locally, which leaves the
remove-confirmation dialog and the whole Offers flow unreachable until the transport phase lands.

**`BarterDao.replaceAll` rewrites every row on every write.** Fine at this size, and the comment says
so, but it means write cost is O(state) rather than O(change).