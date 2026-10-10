# Images

How listing photos work across the two scopes, why the transport differs while the data model does
not, and the sizing budget that makes it feasible.

**Status: design only.** Nothing here is implemented, and nothing here changes the schema. See
[Why nothing is being added to `Listing` yet](#why-nothing-is-being-added-to-listing-yet).

## The shape

A listing carries a content hash and a small inline thumbnail. The bytes are fetched on demand by
a resolver, and there are two of them — one per scope.

```kotlin
interface ImageResolver {
    suspend fun thumbnail(listing: Listing): ImageHandle?
    suspend fun full(listing: Listing): Flow<ByteArray>
}
```

Two implementations behind one interface, mirroring how `BarterRepository` already sits in
`core:domain` with its implementation in `core:data`, selected by the Nearby/Global scope switch:

| | Nearby | Global |
|---|---|---|
| Discovery | Peer-to-peer over Nearby Connections | Nostr relays |
| Bytes from | The owner's phone, live | A blob relay over HTTPS |
| Trust | Proximity, and you can meet them | None — verify the hash or do not render |
| Cost | Bandwidth, and you must stay in range | Somebody hosts the blob |
| Verified how | Same content hash either way | Same content hash either way |

The listing itself is byte-identical in both scopes. Only the resolver differs, which is the whole
point: **the domain model does not know or care which transport carried an image.**

Content-addressing is what makes this coherent. The hash travels inside the listing, so bytes from
an arbitrary blob relay are verifiable, and bytes from a nearby phone are cached under the
identical key. One cache, one key, two fill paths.

## BLE carries discovery, never bytes

Nearby Connections is for finding peers and shaking hands. Bulk transfer over it does not work:
GATT characteristics chunk at roughly 512 bytes, against images of hundreds of kilobytes. You would
hold two phones connected and in range for minutes.

The pattern that actually works is the one Nearby Share uses: **BLE to discover and authenticate,
then upgrade to Wi-Fi Direct or a local hotspot for the payload.** If images are in scope, that
upgrade is not optional, it is the transport.

## Sizing budget

Full-resolution camera files are not useful for browsing a barter listing and will break both
transports. Shrink at capture time, before anything touches the network.

| Asset | Target | Notes |
|---|---|---|
| Thumbnail | ~400px longest edge, JPEG q80 → **~20 KB** | Inlined in the listing; fits under every relay's event cap |
| Full image | Cap at ~1600px, q85 → **~200–400 KB** | Fine over Wi-Fi Direct; a reasonable blob object |

Never upload a camera original. A 12 MP file is wasted bandwidth, wasted storage, and wasted battery
for a grid tile.

## Keeping it out of the user's way

Photos are necessary to the product, so the upload must never be something the user waits on.

- **Resize at capture.** The camera intent returns a downsampled decode, not the full file.
- **Write the thumbnail first.** The listing is created and immediately usable offline.
- **Upload the full image in the background**, with honest progress state in the UI.
- **Wi-Fi-only by default**, pause and resume on cellular, with an obvious per-item override.
- **Never block listing creation** on upload. A listing with a pending image is a valid listing.
- **Cache by content hash, evict by LRU.** One bounded cache serves both scopes.

## Where Global bytes come from

Unresolved, and it is the only part of this design that needs infrastructure. When you are browsing
Global and the owner is not nearby, the bytes have to come from somewhere.

Options, honestly weighed:

- **Volunteer-operated blob relays**, federated like Nostr relays. Fits the project's values and has
  no single operator, but somebody is still running them, and the published relay list becomes a
  real problem — see [security-model.md](security-model.md#keep-out-of-this-repository).
- **Thumbnail-only globally.** Fully serverless and entirely honest, but "images for all" becomes
  "thumbnails for all", and it is a worse product.
- **Hybrid.** Thumbnails everywhere; the full image resolves from whichever peer is online. Degrades
  gracefully, needs no operator, and means Global users sometimes see a thumbnail with no full image
  behind it.

The third is the default here, because it needs no one to commit to running anything. The cost is
that partial state — a thumbnail with no full image — has to be designed as a normal outcome rather
than an error.

## Why nothing is being added to `Listing` yet

The README says reputation has to land in Phase 3 *"before the schema hardens around the idea that a
verified number means something."* Adding `imageHash` and `thumbnail` columns today is precisely
that trap: freezing image fields into schema v1 before the transport and reputation models that
determine their shape exist.

Room is currently at version 1 with `exportSchema = true`, so whatever ships now is written into
`core/data/schemas/dev.barter.core.data.BarterDatabase/1.json` permanently. When images do land,
the schema question should be answered alongside reputation, not ahead of it.

## Storage

Image bytes go into the same Room database as BLOBs, which means
[SQLCipher](security-model.md#encryption-at-rest) covers them with no second encryption layer to
get wrong.

That is fine while the count is small — a user's own items are a handful of images. A future
peer-image cache should move to files and be a separate decision, because unbounded BLOBs in a
rewritten-every-write database is a scaling problem.

## Open questions

- Resolving a full image from a specific peer implies asking for it by hash. What protocol carries
  that request without leaking which listing you are looking at?
- Cached peer images need eviction that respects both storage pressure and the user's privacy
  expectation. "Cached from a nearby phone" is a disclosure worth surfacing.
- Whether a listing can have multiple photos, and whether one is a cover image.