import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import MovieCard from '../components/MovieCard';
import { useAuth } from '../context/AuthContext';
import { movieAPI, listAPI, userAPI } from '../api/client';
import styles from '../styles/home.module.css';

function Home() {
    const [popularMovies, setPopularMovies] = useState([]);
    const [recentMovies, setRecentMovies] = useState([]);
    const [recentlyWatched, setRecentlyWatched] = useState([]);
    const [userLists, setUserLists] = useState([]);
    const [favorites, setFavorites] = useState([]);
    const [selectedList, setSelectedList] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const { currentUser: user } = useAuth();

    useEffect(() => {
        const loadData = async () => {
            try {
                // Fetch Popular Movies
                const popularRes = await movieAPI.getAll({ limit: 20 });
                setPopularMovies(popularRes.data);

                // Fetch Recent Movies
                const recentRes = await movieAPI.getRecent();
                setRecentMovies(recentRes.data);

                // Fetch User Lists and Watch History if logged in
                if (user) {
                    const userId = user.user_id || user.userId;
                    const listsRes = await listAPI.getUserLists(userId);
                    setUserLists(listsRes.data);

                    const historyRes = await movieAPI.getRecentlyWatched(userId, 10);
                    setRecentlyWatched(historyRes.data);

                    const favRes = await userAPI.getFavorites(userId);
                    setFavorites(favRes.data.map(m => m.movie_id));
                }
            } catch (error) {
                console.error('Failed to load home data:', error);
            } finally {
                setLoading(false);
            }
        };

        if (!user) {
            setUserLists([]);
            setRecentlyWatched([]);
            setFavorites([]);
        }

        loadData();
    }, [user]);

    const handleToggleFavorite = async (movieId) => {
        if (!user) {
            alert('Please login to manage favorites');
            return;
        }
        const userId = user.user_id || user.userId;
        try {
            if (favorites.includes(movieId)) {
                await userAPI.removeFromFavorites(userId, movieId);
                setFavorites(prev => prev.filter(id => id !== movieId));
            } else {
                await userAPI.addToFavorites(userId, movieId);
                setFavorites(prev => [...prev, movieId]);
            }
        } catch (error) {
            console.error('Failed to toggle favorite:', error);
        }
    };

    const getPosterUrl = (posterPath) => {
        if (!posterPath) return 'https://via.placeholder.com/150x225?text=No+Poster';
        if (posterPath.startsWith('http')) return posterPath;
        return `https://image.tmdb.org/t/p/w500${posterPath}`;
    };

    const handleListClick = (list) => {
        if (selectedList && selectedList.list_id === list.list_id) {
            setSelectedList(null); // Deselect if already selected
        } else {
            setSelectedList(list);
        }
    };

    if (loading) {
        return (
            <div>
                <Navbar />
                <div className={styles.loading}>Loading...</div>
            </div>
        );
    }

    const heroMovie = popularMovies.length > 0 ? popularMovies[0] : null;

    return (
        <div className={styles.container}>
            <Navbar />

            {/* Simple Hero Section */}
            {heroMovie && (
                <div className={styles.hero}>
                    <img 
                        src={getPosterUrl(heroMovie.poster_path)} 
                        alt={heroMovie.title} 
                        className={styles.heroImage}
                    />
                    <div className={styles.heroOverlay}>
                        <div className={styles.heroContent}>
                            <h1 className={styles.heroTitle}>{heroMovie.title}</h1>
                            <p className={styles.heroOverview}>
                                {heroMovie.overview ? heroMovie.overview.substring(0, 150) + '...' : 'No overview available.'}
                            </p>
                            <button 
                                className={styles.heroButton}
                                onClick={() => navigate(`/movies/${heroMovie.movie_id}`)}
                            >
                                Detayları Gör
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className={styles.content}>

                {/* New Releases Section */}
                <section className={styles.section}>
                    <h2 className={styles.sectionTitle}>New Releases</h2>
                    <div className={styles.scrollRow}>
                        {recentMovies.map((movie) => (
                            <MovieCard
                                key={movie.movie_id}
                                movie={movie}
                                isFavorite={favorites.includes(movie.movie_id)}
                                onToggleFavorite={handleToggleFavorite}
                            />
                        ))}
                    </div>
                </section>

                {/* Recently Watched Section */}
                {user && recentlyWatched.length > 0 && (
                    <section className={styles.section}>
                        <h2 className={styles.sectionTitle}>Recently Watched</h2>
                        <div className={styles.scrollRow}>
                            {recentlyWatched.map((movie) => (
                                <MovieCard
                                    key={movie.movie_id}
                                    movie={movie}
                                    isFavorite={favorites.includes(movie.movie_id)}
                                    onToggleFavorite={handleToggleFavorite}
                                />
                            ))}
                        </div>
                    </section>
                )}

                {/* My Lists Section */}
                {user && (
                    <section className={styles.section}>
                        <h2 className={styles.sectionTitle}>My Lists</h2>

                        {/* Lists Horizontal Scroll/Grid */}
                        <div className={styles.listsGrid} style={{ display: 'flex', gap: '1rem', overflowX: 'auto', paddingBottom: '1rem' }}>
                            {userLists.map(list => (
                                <div
                                    key={list.list_id}
                                    onClick={() => handleListClick(list)}
                                    className={`${styles.listPill} ${selectedList?.list_id === list.list_id ? styles.active : ''}`}
                                >
                                    <h3>{list.list_name}</h3>
                                    <p>{list.movies?.length || 0} movies</p>
                                </div>
                            ))}
                            {userLists.length === 0 && (
                                <p style={{ color: '#ccc' }}>No lists created yet.</p>
                            )}
                        </div>

                        {/* Selected List Movies */}
                        {selectedList && (
                            <div className={styles.selectedListContainer}>
                                <h3>Movies in: {selectedList.list_name}</h3>
                                <div className={styles.movieGrid}>
                                    {selectedList.movies && selectedList.movies.length > 0 ? (
                                        selectedList.movies.map(movie => (
                                            <div
                                                key={movie.movie_id}
                                                className={styles.movieCard}
                                                onClick={() => navigate(`/movies/${movie.movie_id}`)}
                                            >
                                                <img
                                                    src={getPosterUrl(movie.poster_path)}
                                                    alt={movie.title}
                                                    className={styles.poster}
                                                />
                                                <div className={styles.movieInfo}>
                                                    <h3 className={styles.movieTitle}>{movie.title}</h3>
                                                </div>
                                            </div>
                                        ))
                                    ) : (
                                        <p>No movies in this list.</p>
                                    )}
                                </div>
                            </div>
                        )}
                    </section>
                )}

                {/* Popular Movies Section */}
                <section className={styles.section}>
                    <h2 className={styles.sectionTitle}>Popular Movies</h2>
                    <div className={styles.movieGrid}>
                        {popularMovies.map((movie) => (
                            <div
                                key={movie.movie_id}
                                className={styles.movieCard}
                                onClick={() => navigate(`/movies/${movie.movie_id}`)}
                            >
                                <img
                                    src={getPosterUrl(movie.poster_path)}
                                    alt={movie.title}
                                    className={styles.poster}
                                />
                                <div className={styles.movieInfo}>
                                    <h3 className={styles.movieTitle}>{movie.title}</h3>
                                    <div className={styles.rating}>⭐ {movie.rating?.toFixed(1) || 'N/A'}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>
            </div>
        </div>
    );
}

export default Home;
