import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import MovieCard from '../components/MovieCard';
import { useAuth } from '../context/AuthContext';
import { movieAPI, userAPI } from '../api/client';
import './Explore.css'; 
import homeStyles from '../styles/home.module.css'; 

function Movies() {
    const [movies, setMovies] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [genres, setGenres] = useState([]);
    const [selectedGenres, setSelectedGenres] = useState([]);
    const [showGenreFilter, setShowGenreFilter] = useState(false);
    const [favorites, setFavorites] = useState([]);
    
    const [sortBy, setSortBy] = useState('popularity');
    const [order, setOrder] = useState('DESC');

    const navigate = useNavigate();
    const { currentUser: user } = useAuth();

    const observer = useRef();
    const lastMovieElementRef = useCallback(node => {
        if (loading) return;
        if (observer.current) observer.current.disconnect();
        observer.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMore) {
                setPage(prevPage => prevPage + 1);
            }
        });
        if (node) observer.current.observe(node);
    }, [loading, hasMore]);

    useEffect(() => {
        loadGenres();
        if (user) {
            loadFavorites();
        }
    }, [user]);

    const loadFavorites = async () => {
        const userId = user.user_id || user.userId;
        try {
            const response = await userAPI.getFavorites(userId);
            setFavorites(response.data.map(m => m.movie_id));
        } catch (error) {
            console.error('Failed to load favorites:', error);
        }
    };

    const loadGenres = async () => {
        try {
            const response = await movieAPI.getGenres();
            setGenres(response.data);
        } catch (error) {
            console.error('Failed to load genres:', error);
        }
    };

    const fetchMovies = async (pageNum, reset = false) => {
        setLoading(true);
        try {
            const params = { page: pageNum, limit: 20, sortBy, order };
            let responses = [];

            if (searchQuery.trim()) {
                const res = await movieAPI.search(searchQuery, params);
                responses = [res];
            } else if (selectedGenres.length > 0) {
                const promises = selectedGenres.map(genre => movieAPI.getByGenre(genre, params));
                responses = await Promise.all(promises);
            } else {
                const res = await movieAPI.getAll(params);
                responses = [res];
            }

            const allMovies = responses.flatMap(r => r.data);
            const uniqueMovies = Array.from(
                new Map(allMovies.map(m => [m.movie_id, m])).values()
            );

            if (reset) {
                setMovies(uniqueMovies);
            } else {
                setMovies(prev => {
                    const existingIds = new Set(prev.map(m => m.movie_id));
                    const newUnique = uniqueMovies.filter(m => !existingIds.has(m.movie_id));
                    return [...prev, ...newUnique];
                });
            }
            
            // Eğer dönen sonuç yoksa veya tekil sorguda limite ulaşmadıysa hasMore kapatılır
            setHasMore(uniqueMovies.length > 0);
        } catch (error) {
            console.error('Failed to fetch movies:', error);
        } finally {
            setLoading(false);
        }
    };

    // Filtre veya sıralama değiştiğinde sayfayı sıfırla ve yeniden çek
    useEffect(() => {
        setPage(1);
        setHasMore(true);
        fetchMovies(1, true);
    }, [sortBy, order, selectedGenres]);

    // Sayfa (page) değiştiğinde, eğer 1'den büyükse sonuna ekle
    useEffect(() => {
        if (page > 1) {
            fetchMovies(page, false);
        }
    }, [page]);

    const handleSearch = (e) => {
        e.preventDefault();
        setPage(1);
        setHasMore(true);
        fetchMovies(1, true);
    };

    const toggleGenre = (genre) => {
        setSelectedGenres(prev =>
            prev.includes(genre)
                ? prev.filter(g => g !== genre)
                : [...prev, genre]
        );
    };

    const handleToggleFavorite = async (movieId) => {
        if (!user) {
            alert('Please login to manage favorites');
            return;
        }
        const userId = user.user_id || user.userId;
        try {
            if (favorites && favorites.includes(movieId)) {
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

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#0a0a0a' }}>
            <Navbar />
            <div className="explore-page">
                <div className="search-section" style={{ marginTop: '0', padding: '0' }}>
                    <div className="search-container">
                        <h1 style={{ color: 'white', marginBottom: '20px' }}>All Movies</h1>
                        <form onSubmit={handleSearch} className="search-form">
                            <input
                                type="text"
                                placeholder="Search movies..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="search-input"
                            />
                            <button type="submit" className="search-button">Search</button>
                            
                            <button
                                type="button"
                                className="filter-button"
                                style={{ backgroundColor: '#e50914', color: 'white' }}
                                onClick={() => setShowGenreFilter(!showGenreFilter)}
                            >
                                {showGenreFilter ? 'Close Filter' : 'Filter by Genre'}
                            </button>

                            <select 
                                value={`${sortBy}-${order}`} 
                                onChange={(e) => {
                                    const [newSort, newOrder] = e.target.value.split('-');
                                    setSortBy(newSort);
                                    setOrder(newOrder);
                                }}
                                className="filter-button"
                                style={{ backgroundColor: '#333', color: 'white', border: '1px solid #555', padding: '0 15px', cursor: 'pointer' }}
                            >
                                <option value="popularity-DESC">Popülerlik</option>
                                <option value="title-ASC">A-Z</option>
                                <option value="title-DESC">Z-A</option>
                                <option value="rating-DESC">Puan (10-0)</option>
                                <option value="rating-ASC">Puan (0-10)</option>
                            </select>
                        </form>
                    </div>
                </div>

                {showGenreFilter && (
                    <div className="genre-filter-panel">
                        <h3>Select Genres</h3>
                        <div className="genre-grid">
                            {genres.map(genre => (
                                <label key={genre} className="genre-checkbox">
                                    <input
                                        type="checkbox"
                                        checked={selectedGenres.includes(genre)}
                                        onChange={() => toggleGenre(genre)}
                                    />
                                    <span>{genre}</span>
                                </label>
                            ))}
                        </div>
                        <div className="filter-actions">
                            <button onClick={() => {
                                setSelectedGenres([]);
                                setShowGenreFilter(false);
                            }} className="clear-button">Clear Filters</button>
                        </div>
                    </div>
                )}

                <div className="results-section">
                    <div className={homeStyles.movieGrid} style={{ padding: '20px', maxWidth: '1400px', margin: '0 auto' }}>
                        {movies.map((movie, index) => {
                            if (movies.length === index + 1) {
                                return (
                                    <div ref={lastMovieElementRef} key={movie.movie_id}>
                                        <MovieCard
                                            movie={movie}
                                            isFavorite={favorites ? favorites.includes(movie.movie_id) : false}
                                            onToggleFavorite={handleToggleFavorite}
                                        />
                                    </div>
                                );
                            } else {
                                return (
                                    <MovieCard
                                        key={movie.movie_id}
                                        movie={movie}
                                        isFavorite={favorites ? favorites.includes(movie.movie_id) : false}
                                        onToggleFavorite={handleToggleFavorite}
                                    />
                                );
                            }
                        })}
                    </div>

                    {loading && (
                        <div className="loading" style={{ textAlign: 'center', padding: '20px', color: '#888' }}>
                            Loading more movies...
                        </div>
                    )}

                    {!loading && movies.length === 0 && (
                        <p className="no-results" style={{ textAlign: 'center', color: '#888' }}>No movies found</p>
                    )}

                    {!loading && !hasMore && movies.length > 0 && (
                        <p className="no-results" style={{ textAlign: 'center', color: '#888', marginTop: '20px' }}>
                            You have reached the end of the list.
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}

export default Movies;
