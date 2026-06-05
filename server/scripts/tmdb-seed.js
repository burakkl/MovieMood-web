/**
 * TMDB Film Seed Script — ~20.000 Dengeli Film
 * 
 * Kullanım:
 *   1. server/.env dosyasına DATABASE_URL ve TMDB_API_KEY ekle
 *   2. cd server && node scripts/tmdb-seed.js
 * 
 * Strateji:
 *   - Eski kült filmler (1950-1979): vote_count >= 100 → meşhur klasikler
 *   - Nostalji dönemi (1980-1999): vote_count >= 50 → popüler filmler
 *   - Modern dönem (2000-2026): vote_count >= 30 → geniş yelpaze
 *   - Toplam hedef: ~20.000 film
 */

import pg from 'pg';
import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// .env dosyasını server/ dizininden oku
dotenv.config({ path: join(__dirname, '..', '.env') });

const { Pool } = pg;

// ─── Config ────────────────────────────────────────────────────────────────

const TMDB_API_KEY = process.env.TMDB_API_KEY || 'b94045aa0416dc579183b96ac34cfb16';
const TMDB_BASE = 'https://api.themoviedb.org/3';
const REQUEST_DELAY_MS = 350; // ~3 req/s (TMDB limiti altında)
const MAX_PAGES_PER_QUERY = 500;

const GENRE_MAP = {
  28: 'Action', 12: 'Adventure', 16: 'Animation', 35: 'Comedy',
  80: 'Crime', 99: 'Documentary', 18: 'Drama', 10751: 'Family',
  14: 'Fantasy', 36: 'History', 27: 'Horror', 10402: 'Music',
  9648: 'Mystery', 10749: 'Romance', 878: 'Science Fiction',
  10770: 'TV Movie', 53: 'Thriller', 10752: 'War', 37: 'Western'
};

// Dengeli dağılım: eski = yüksek vote threshold, yeni = düşük
const YEAR_RANGES = [
  // Kült klasikler (1950-1979) — sadece gerçekten meşhur olanlar
  { startYear: 1950, endYear: 1979, minVotes: 100, label: '🎬 Kült Klasikler' },
  // Nostalji dönemi (1980-1999) — popüler filmler
  { startYear: 1980, endYear: 1999, minVotes: 50, label: '📼 Nostalji Dönemi' },
  // Modern dönem (2000-2014) — geniş yelpaze
  { startYear: 2000, endYear: 2014, minVotes: 30, label: '🎥 Modern Dönem (Erken)' },
  // Güncel dönem (2015-2026) — en geniş
  { startYear: 2015, endYear: 2026, minVotes: 20, label: '🍿 Güncel Dönem' },
];

// ─── Database ──────────────────────────────────────────────────────────────

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false },
  max: 5,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
});

// ─── Helpers ───────────────────────────────────────────────────────────────

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function fetchTMDB(endpoint, params = {}) {
  const url = new URL(`${TMDB_BASE}${endpoint}`);
  url.searchParams.set('api_key', TMDB_API_KEY);
  url.searchParams.set('language', 'en-US');
  for (const [key, value] of Object.entries(params)) {
    url.searchParams.set(key, value);
  }

  const response = await fetch(url.toString());
  if (!response.ok) {
    if (response.status === 429) {
      // Rate limited — wait and retry
      console.log('  ⏳ Rate limited, waiting 10s...');
      await sleep(10000);
      return fetchTMDB(endpoint, params);
    }
    throw new Error(`TMDB API error: ${response.status} ${response.statusText}`);
  }
  return response.json();
}

async function insertMovie(client, movie) {
  const year = movie.release_date ? parseInt(movie.release_date.substring(0, 4)) : null;

  await client.query(
    `INSERT INTO movies (movie_id, title, original_title, overview, language, release_date,
      rating, vote_count, poster_path, backdrop_path, popularity, web_link, youtube_link)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
     ON CONFLICT (movie_id) DO NOTHING`,
    [
      movie.id,
      movie.title,
      movie.original_title,
      movie.overview,
      movie.original_language,
      year,
      movie.vote_average,
      movie.vote_count,
      movie.poster_path,
      movie.backdrop_path,
      movie.popularity,
      null, // web_link — sonra IMDB'den çekilebilir
      null  // youtube_link — sonra eklenebilir
    ]
  );

  // Genre'ları ekle
  for (const genreId of (movie.genre_ids || [])) {
    const genreName = GENRE_MAP[genreId];
    if (genreName) {
      await client.query(
        'INSERT INTO movie_genres (movie_id, genre) VALUES ($1, $2) ON CONFLICT DO NOTHING',
        [movie.id, genreName]
      );
    }
  }
}

// ─── Ana Seed Fonksiyonu ───────────────────────────────────────────────────

async function seedYearRange(range) {
  const { startYear, endYear, minVotes, label } = range;
  console.log(`\n${'═'.repeat(60)}`);
  console.log(`${label} (${startYear}-${endYear}, min ${minVotes} votes)`);
  console.log('═'.repeat(60));

  let totalInserted = 0;
  const client = await pool.connect();

  try {
    for (let year = endYear; year >= startYear; year--) {
      // İlk sayfayı çek — toplam sayfa sayısını öğren
      const firstPage = await fetchTMDB('/discover/movie', {
        primary_release_year: year,
        sort_by: 'popularity.desc',
        'vote_count.gte': minVotes,
        page: 1,
        include_adult: false,
      });

      const totalPages = Math.min(firstPage.total_pages || 0, MAX_PAGES_PER_QUERY);
      const totalResults = firstPage.total_results || 0;

      if (totalResults === 0) {
        console.log(`  ${year}: Film yok, atlanıyor`);
        continue;
      }

      console.log(`  ${year}: ${totalResults} film bulundu (${totalPages} sayfa)`);

      // İlk sayfanın filmlerini ekle
      for (const movie of (firstPage.results || [])) {
        await insertMovie(client, movie);
        totalInserted++;
      }

      // Geri kalan sayfaları çek
      for (let page = 2; page <= totalPages; page++) {
        await sleep(REQUEST_DELAY_MS);

        try {
          const data = await fetchTMDB('/discover/movie', {
            primary_release_year: year,
            sort_by: 'popularity.desc',
            'vote_count.gte': minVotes,
            page: page,
            include_adult: false,
          });

          for (const movie of (data.results || [])) {
            await insertMovie(client, movie);
            totalInserted++;
          }

          // Her 10 sayfada progress göster
          if (page % 10 === 0) {
            console.log(`    📄 Sayfa ${page}/${totalPages} — toplam: ${totalInserted}`);
          }
        } catch (err) {
          console.error(`    ❌ Sayfa ${page} hatası: ${err.message}`);
          // Hata olursa devam et, durma
        }
      }

      await sleep(REQUEST_DELAY_MS);
    }
  } finally {
    client.release();
  }

  console.log(`  ✅ ${label}: ${totalInserted} film eklendi`);
  return totalInserted;
}

async function main() {
  console.log('🎬 MovieMood TMDB Seed Script');
  console.log('─'.repeat(60));

  // DB bağlantı testi
  try {
    const testResult = await pool.query('SELECT COUNT(*) FROM movies');
    const existingCount = parseInt(testResult.rows[0].count);
    console.log(`📊 Mevcut film sayısı: ${existingCount}`);
  } catch (err) {
    console.error('❌ Veritabanı bağlantı hatası:', err.message);
    console.error('DATABASE_URL doğru mu kontrol edin!');
    process.exit(1);
  }

  // TMDB API testi
  try {
    const test = await fetchTMDB('/movie/popular', { page: 1 });
    console.log(`✅ TMDB API bağlantısı başarılı (${test.total_results} popüler film)`);
  } catch (err) {
    console.error('❌ TMDB API hatası:', err.message);
    console.error('TMDB_API_KEY doğru mu kontrol edin!');
    process.exit(1);
  }

  const startTime = Date.now();
  let grandTotal = 0;

  for (const range of YEAR_RANGES) {
    const count = await seedYearRange(range);
    grandTotal += count;
    console.log(`\n📈 Toplam şu ana kadar: ${grandTotal} film`);
  }

  // Son durum raporu
  const elapsed = ((Date.now() - startTime) / 1000 / 60).toFixed(1);
  const finalCount = await pool.query('SELECT COUNT(*) FROM movies');
  const genreCount = await pool.query('SELECT COUNT(DISTINCT genre) FROM movie_genres');

  console.log('\n' + '═'.repeat(60));
  console.log('🎉 SEED TAMAMLANDI!');
  console.log('═'.repeat(60));
  console.log(`  📊 Veritabanındaki toplam film: ${finalCount.rows[0].count}`);
  console.log(`  🎭 Toplam genre sayısı: ${genreCount.rows[0].count}`);
  console.log(`  ⏱️  Süre: ${elapsed} dakika`);
  console.log(`  💾 Bu sefer eklenen: ${grandTotal} film`);
  console.log('═'.repeat(60));

  await pool.end();
  process.exit(0);
}

main().catch(err => {
  console.error('💀 Fatal error:', err);
  process.exit(1);
});
