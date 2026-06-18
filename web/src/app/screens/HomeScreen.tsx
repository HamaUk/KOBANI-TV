// Simplified Home screen for Optic TV:
//   1. Live TV
//   2. Sports
//   3. Movies
//   4. Series

import { useLiveQuery } from "dexie-react-hooks";
import { Link } from "react-router-dom";
import {
  channelRepo,
  movieRepo,
  seriesRepo,
} from "@data/db/repositories";
import { useEffect } from "react";
import { useProviderStore } from "@app/stores/providers";
import { useCategoryFilters } from "@app/stores/categoryFilters";
import { PosterCard } from "@app/components/PosterCard";
import { ProxyImg } from "@app/components/ProxyImg";
import { useI18n } from "@app/i18n";
import { Skeleton } from "@app/components/Skeleton";
import type { Channel } from "@domain/model";

function isSportChannel(c: Channel) {
  const g = (c.groupTitle || "").toLowerCase();
  const n = (c.name || "").toLowerCase();
  const sportKeywords = [
    'sport', 'bein', 'ad sports', 'ssc', 'eurospot', 'espn', 
    'arena', 'bt sport', 'sky sport', 'alkass', 'starzplay sports'
  ];
  return sportKeywords.some(kw => g.includes(kw) || n.includes(kw));
}

export function HomeScreen() {
  const providerId = useProviderStore((s) => s.activeProviderId);
  const t = useI18n((s) => s.t);
  const loadFilters = useCategoryFilters((s) => s.load);
  const liveAllowed = useCategoryFilters((s) => providerId == null ? null : s.allowed[`${providerId}:LIVE`]);
  const movieAllowed = useCategoryFilters((s) => providerId == null ? null : s.allowed[`${providerId}:MOVIE`]);
  const seriesAllowed = useCategoryFilters((s) => providerId == null ? null : s.allowed[`${providerId}:SERIES`]);

  useEffect(() => { if (providerId != null) void loadFilters(providerId); }, [providerId, loadFilters]);

  const passesLive = (categoryId: number | null) =>
    liveAllowed == null ? true : categoryId != null && liveAllowed.includes(categoryId);
  const passesMovie = (categoryId: number | null) =>
    movieAllowed == null ? true : categoryId != null && movieAllowed.includes(categoryId);
  const passesSeries = (categoryId: number | null) =>
    seriesAllowed == null ? true : categoryId != null && seriesAllowed.includes(categoryId);

  const movies = useLiveQuery(async () => providerId == null ? [] : movieRepo.forProvider(providerId), [providerId]);
  const series = useLiveQuery(async () => providerId == null ? [] : seriesRepo.forProvider(providerId), [providerId]);
  const channels = useLiveQuery(async () => providerId == null ? [] : channelRepo.forProvider(providerId), [providerId]);

  if (providerId == null) return <div className="empty">{t("home.noProvider")}</div>;
  if (movies === undefined || series === undefined || channels === undefined) {
    return (
      <div>
        <h2 style={{ marginTop: 0 }}>{t("nav.home")}</h2>
        <Skeleton variant="shelf" count={6} />
        <div style={{ height: 24 }} />
        <Skeleton variant="shelf" count={6} />
      </div>
    );
  }

  const filteredMovies = movies.filter((m) => passesMovie(m.categoryId)).slice(0, 20);
  const filteredSeries = series.filter((s) => passesSeries(s.categoryId)).slice(0, 20);
  const allLive = channels.filter((c) => passesLive(c.categoryId));
  
  const liveSlice = allLive.filter(c => !isSportChannel(c)).slice(0, 20);
  const sportsSlice = allLive.filter(isSportChannel).slice(0, 20);

  const sectionStyle = { marginBottom: 28 } as const;

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>{t("nav.home")}</h2>

      {liveSlice.length > 0 && (
        <section style={sectionStyle}>
          <h3>{t("nav.live")}</h3>
          <div className="shelf">
            {liveSlice.map((c) => (
              <Link key={c.id} to="/live" style={{ textDecoration: "none", color: "inherit" }}>
                <div className="poster-card" style={{ width: 140 }}>
                  <div className="poster-img" style={{ width: 140, height: 140, borderRadius: 70 }}>
                    <ProxyImg src={c.logoUrl} alt="" style={{ width: "70%", height: "70%", objectFit: "contain" }} fallback={<span className="poster-fallback">{c.name.slice(0, 2)}</span>} />
                  </div>
                  <div className="poster-title" style={{ textAlign: "center" }}>{c.name}</div>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {sportsSlice.length > 0 && (
        <section style={sectionStyle}>
          <h3>{t("nav.sports")}</h3>
          <div className="shelf">
            {sportsSlice.map((c) => (
              <Link key={c.id} to="/sports" style={{ textDecoration: "none", color: "inherit" }}>
                <div className="poster-card" style={{ width: 140 }}>
                  <div className="poster-img" style={{ width: 140, height: 140, borderRadius: 70 }}>
                    <ProxyImg src={c.logoUrl} alt="" style={{ width: "70%", height: "70%", objectFit: "contain" }} fallback={<span className="poster-fallback">{c.name.slice(0, 2)}</span>} />
                  </div>
                  <div className="poster-title" style={{ textAlign: "center" }}>{c.name}</div>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {filteredMovies.length > 0 && (
        <section style={sectionStyle}>
          <h3>{t("nav.movies")}</h3>
          <div className="shelf">
            {filteredMovies.map((m) => (
              <Link key={m.id} to={`/movies/${m.id}`} style={{ textDecoration: "none", color: "inherit" }}>
                <PosterCard posterUrl={m.posterUrl} title={m.name} subtitle={m.year ?? null} />
              </Link>
            ))}
          </div>
        </section>
      )}

      {filteredSeries.length > 0 && (
        <section style={sectionStyle}>
          <h3>{t("nav.series")}</h3>
          <div className="shelf">
            {filteredSeries.map((s) => (
              <Link key={s.id} to={`/series/${s.id}`} style={{ textDecoration: "none", color: "inherit" }}>
                <PosterCard posterUrl={s.posterUrl} title={s.name} />
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
