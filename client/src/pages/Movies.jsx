import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import MovieCard from '../components/MovieCard';
import { useAuth } from '../context/AuthContext';
import { movieAPI, userAPI } from '../api/client';
import './Explore.css'; // Reusing Explore styles for consistent UI
import homeStyles from '../styles/home.module.css'; // For MovieCard grid

function Movies() {
    const [movies, setMovies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const [genres, setGenres] = useState([]);
    const [selectedGenres, setSelectedGenres] = useState([]);
    const [showGenreFilter, setShowGenreFilter] = useState(false);
    const [favorites, setFavorites] = useState([]);
    const navigate = useNavigate();
    const { currentUser: user } = useAuth();

    useEffect(() => {
        loadGenres();
        loadRandomMovies();
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

    const loadRandomMovies = async () => {
        setLoading(true);
        try {
            const response = await movieAPI.getAll({ limit: 100 });
            setMovies(response.data);
        } catch (error) {
            console.error('Failed to load movies:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            if (!searchQuery.trim()) {
                await loadRandomMovies();
                return;
            }
            const response = await movieAPI.search(searchQuery);
            setMovies(response.data);
        } catch (error) {
            console.error('Search failed:', error);
        } finally {
            setLoading(false);
        }
    };

    const toggleGenre = (genre) => {
        setSelectedGenres(prev =>
            prev.includes(genre)
                ? prev.filter(g => g !== genre)
                : [...prev, genre]
        );
    };

    const applyGenreFilter = async () => {
        if (selectedGenres.length === 0) {
            loadRandomMovies();
            setShowGenreFilter(false);
            return;
        }

        setLoading(true);
        try {
            const promises = selectedGenres.map(genre => movieAPI.getByGenre(genre));
            const responses = await Promise.all(promises);

            const allMovies = responses.flatMap(r => r.data);
            const uniqueMovies = Array.from(
                new Map(allMovies.map(m => [m.movie_id, m])).values()
            );

            setMovies(uniqueMovies);
            setShowGenreFilter(false);
        } catch (error) {
            console.error('Failed to filter by genre:', error);
        } finally {
            setLoading(false);
        }
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
                            <button onClick={applyGenreFilter} className="apply-button">Apply Filter</button>
                            <button onClick={() => {
                                setSelectedGenres([]);
                                loadRandomMovies();
                                setShowGenreFilter(false);
                            }} className="clear-button">Clear</button>
                        </div>
                    </div>
                )}

                <div className="results-section">
                    {loading ? (
                        <div className="loading">Loading...</div>
                    ) : (
                        <div className={homeStyles.movieGrid} style={{ padding: '20px', maxWidth: '1400px', margin: '0 auto' }}>
                            {movies.map(movie => (
                                <MovieCard
                                    key={movie.movie_id}
                                    movie={movie}
                                    isFavorite={favorites ? favorites.includes(movie.movie_id) : false}
                                    onToggleFavorite={handleToggleFavorite}
                                />
                            ))}
                        </div>
                    )}

                    {!loading && movies.length === 0 && (
                        <p className="no-results">No movies found</p>
                    )}
                </div>
            </div>
        </div>
    );
}

export default Movies;
