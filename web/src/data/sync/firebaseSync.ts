import { ref, get } from "firebase/database";
import { rtdb } from "@data/firebase";
import { db } from "@data/db/database";
import type { Channel, Movie, Series, Category } from "@domain/model";

// Firebase RTDB Channel Model based on optic_tv
interface FirebaseChannel {
  name?: string;
  url?: string;
  group?: string;
  category?: string;
  logo?: string;
  icon_url?: string;
  type?: string;
  featured?: boolean;
  order?: number;
  featured_order?: number;
  userAgent?: string;
  user_agent?: string;
}

export async function syncFirebasePlaylist() {
  try {
    const snapshot = await get(ref(rtdb, 'sync/global/managedPlaylist'));
    if (!snapshot.exists()) return;

    const data = snapshot.val();
    let items: FirebaseChannel[] = [];
    if (Array.isArray(data)) {
      items = data.filter(Boolean) as FirebaseChannel[];
    } else if (typeof data === 'object') {
      items = Object.values(data) as FirebaseChannel[];
    }

    // Ensure we have a default provider ID (we can use 1)
    const providerId = 1;

    const channels: Omit<Channel, "id">[] = [];
    const movies: Omit<Movie, "id">[] = [];
    const series: Omit<Series, "id">[] = [];
    const categories: Omit<Category, "id">[] = [];

    const categoryMap = new Map<string, number>(); // Name -> Category ID offset
    let nextCatId = 1;

    const getCategoryId = (name: string, type: "LIVE" | "MOVIE" | "SERIES") => {
      const key = `${type}_${name}`;
      if (categoryMap.has(key)) return categoryMap.get(key)!;
      const id = nextCatId++;
      categories.push({
        providerId,
        roomId: 0,
        name,
        parentId: null,
        type,
        isVirtual: false,
        count: 0,
        isAdult: false,
        isUserProtected: false
      });
      categoryMap.set(key, id);
      return id;
    };

    let order = 0;
    for (const item of items) {
      if (!item.url || !item.name) continue;
      
      const typeStr = item.type || 'live';
      const groupName = item.group || item.category || 'General';
      const logoUrl = item.logo || item.icon_url || null;

      order++;

      if (typeStr === 'movie') {
        const catId = getCategoryId(groupName, "MOVIE");
        movies.push({
          providerId,
          categoryId: catId,
          categoryName: groupName,
          name: item.name,
          posterUrl: logoUrl,
          backdropUrl: null,
          streamUrl: item.url,
          containerExtension: null,
          plot: null,
          cast: null,
          director: null,
          genre: null,
          releaseDate: null,
          duration: null,
          durationSeconds: 0,
          rating: 0,
          year: null,
          tmdbId: null,
          youtubeTrailer: null,
          isFavorite: false,
          watchProgress: 0,
          lastWatchedAt: 0,
          isAdult: false,
          isUserProtected: false,
          streamId: order,
          addedAt: Date.now()
        });
      } else if (typeStr === 'series') {
        const catId = getCategoryId(groupName, "SERIES");
        series.push({
          providerId,
          categoryId: catId,
          categoryName: groupName,
          name: item.name,
          posterUrl: logoUrl,
          backdropUrl: null,
          plot: null,
          cast: null,
          director: null,
          genre: null,
          releaseDate: null,
          rating: 0,
          tmdbId: null,
          youtubeTrailer: null,
          isFavorite: false,
          seasons: [],
          episodeRunTime: null,
          lastModified: Date.now(),
          isAdult: false,
          isUserProtected: false,
          seriesId: order,
          providerSeriesId: null
        });
      } else {
        const catId = getCategoryId(groupName, "LIVE");
        channels.push({
          providerId,
          categoryId: catId,
          categoryName: groupName,
          name: item.name,
          canonicalName: item.name.toLowerCase().replace(/[^a-z0-9]/g, ''),
          logoUrl: logoUrl,
          groupTitle: groupName,
          streamUrl: item.url,
          epgChannelId: null,
          number: order,
          isFavorite: false,
          catchUpSupported: false,
          catchUpDays: 0,
          catchUpSource: null,
          currentProgram: null,
          nextProgram: null,
          isAdult: false,
          isUserProtected: false,
          logicalGroupId: `live_${order}`,
          selectedVariantId: 0,
          errorCount: 0,
          qualityOptions: [],
          alternativeStreams: [],
          streamId: order,
          variants: [{
            rawChannelId: order,
            logicalGroupId: `live_${order}`,
            providerId,
            originalName: item.name,
            canonicalName: item.name.toLowerCase().replace(/[^a-z0-9]/g, ''),
            streamUrl: item.url,
            streamId: order,
            epgChannelId: null,
            number: order,
            errorCount: 0,
            catchUpSupported: false,
            catchUpDays: 0,
            catchUpSource: null,
            attributes: {
              resolutionLabel: null, declaredHeight: null, qualityTier: 1, codecLabel: null,
              transportLabel: null, frameRate: null, isHdr: false, sourceHint: null,
              regionHint: null, languageHint: null, rawTags: []
            },
            observedQuality: { lastObservedWidth: 0, lastObservedHeight: 0, lastObservedBitrate: 0, lastObservedFrameRate: 0, successCount: 0, lastSuccessfulAt: 0 }
          }]
        });
      }
    }

    await db.transaction("rw", [db.providers, db.channels, db.movies, db.series, db.categories], async () => {
      // Ensure pseudo-provider exists
      const existingProv = await db.providers.get(providerId);
      if (!existingProv) {
        await db.providers.put({
          id: providerId,
          name: "Firebase Provider",
          type: "M3U", // Doesn't matter much
          serverUrl: "", username: "", password: "", m3uUrl: "", epgUrl: "",
          stalkerMacAddress: "", stalkerDeviceProfile: "", stalkerDeviceTimezone: "", stalkerDeviceLocale: "",
          userAgent: "", httpReferer: "", isActive: true, maxConnections: 1,
          expirationDate: null, apiVersion: null, allowedOutputFormats: [],
          epgSyncMode: "SKIP", xtreamFastSyncEnabled: false, m3uVodClassificationEnabled: false,
          status: "ACTIVE", lastSyncedAt: Date.now(), createdAt: Date.now()
        });
      }

      await db.channels.where("providerId").equals(providerId).delete();
      await db.movies.where("providerId").equals(providerId).delete();
      await db.series.where("providerId").equals(providerId).delete();
      await db.categories.where("providerId").equals(providerId).delete();

      if (categories.length > 0) await db.categories.bulkAdd(categories as Category[]);
      if (channels.length > 0) await db.channels.bulkAdd(channels as Channel[]);
      if (movies.length > 0) await db.movies.bulkAdd(movies as Movie[]);
      if (series.length > 0) await db.series.bulkAdd(series as Series[]);
      
      await db.providers.update(providerId, { lastSyncedAt: Date.now(), status: "ACTIVE" });
    });

  } catch (error) {
    console.error("Firebase sync error", error);
  }
}
