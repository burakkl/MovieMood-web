import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { movieAPI, listAPI, userAPI } from '../api/client';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import './MovieDetails.css';

function MovieDetails() {
    const { id } = useParams();
    const [movie, setMovie] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showRatingModal, setShowRatingModal] = useState(false);
    const [showCommentsModal, setShowCommentsModal] = useState(false);
    const [showAddToListModal, setShowAddToListModal] = useState(false);
    const [rating, setRating] = useState(5);
    const [comment, setComment] = useState('');
    const [userLists, setUserLists] = useState([]);
    const [comments, setComments] = useState([]);
    const [isFavorite, setIsFavorite] = useState(false);
    const navigate = useNavigate();
    const { currentUser: user } = useAuth(); // Use AuthContext instead of localStorage

    useEffect(() => {
        loadMovie();
        if (user) {
            loadUserLists();
            markAsWatched();
            checkFavoriteStatus();
        }
    }, [id, user]); // Add user to dependency array

    const checkFavoriteStatus = async () => {
        try {
            const userId = user.userId || user.user_id;
            const response = await userAPI.getFavorites(userId);
            const favorites = response.data;
            const isFav = favorites.some(m => m.movie_id === parseInt(id));
            setIsFavorite(isFav);
        } catch (error) {
            console.error('Failed to check favorite status:', error);
        }
    };

    const loadMovie = async () => {
        try {
            const response = await movieAPI.getById(id);
            setMovie(response.data);
            loadComments();
        } catch (error) {
            console.error('Failed to load movie:', error);
        } finally {
            setLoading(false);
        }
    };

    const loadComments = async () => {
        try {
            const response = await movieAPI.getComments(id);
            setComments(response.data);
        } catch (error) {
            console.error('Failed to load comments:', error);
        }
    };

    const loadUserLists = async () => {
        try {
            const response = await listAPI.getUserLists(user.userId || user.user_id);
            setUserLists(response.data);
        } catch (error) {
            console.error('Failed to load lists:', error);
        }
    };

    const markAsWatched = async () => {
        try {
            await movieAPI.markAsWatched(id, user.userId || user.user_id);
        } catch (error) {
            console.error('Failed to mark as watched:', error);
        }
    };

    const handleRating = async (e) => {
        e.preventDefault();
        try {
            await movieAPI.rateMovie(id, user.userId || user.user_id, rating);
            setShowRatingModal(false);
            loadMovie();
            alert('Rating submitted!');
        } catch (error) {
            console.error('Failed to rate:', error);
        }
    };

    const handleAddComment = async (e) => {
        if (e) e.preventDefault();
        console.log('Posting comment:', comment);

        const userId = user?.userId || user?.user_id;
        if (!user || !userId) {
            console.error('User not logged in');
            alert('Please login to comment');
            return;
        }

        if (!comment.trim()) {
            console.log('Comment empty');
            return;
        }

        try {
            await movieAPI.addComment(id, userId, comment);
            setComment('');
            loadComments();
            console.log('Comment posted successfully');
        } catch (error) {
            console.error('Failed to add comment:', error);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!confirm('Delete this comment?')) return;

        try {
            await movieAPI.deleteComment(id, commentId, user.userId || user.user_id);
            loadComments();
        } catch (error) {
            console.error('Failed to delete comment:', error);
        }
    };

    const handleAddToList = async (listId) => {
        try {
            await listAPI.addMovieToList(listId, id);
            setShowAddToListModal(false);
            alert('Added to list!');
        } catch (error) {
            alert(error.response?.data?.error || 'Failed to add to list');
        }
    };

    const openIMDb = () => {
        if (movie.web_link) {
            window.open(movie.web_link, '_blank');
        }
    };

    const handleToggleFavorite = async () => {
        if (!user) {
            alert('Please login to manage favorites');
            return;
        }

        const userId = user.userId || user.user_id;
        const movieId = parseInt(id);

        if (!userId) {
            console.error('User ID is missing on client side');
            alert('Authentication error. Please login again.');
            return;
        }

        if (isNaN(movieId)) {
            console.error('Invalid Movie ID:', id);
            alert('Invalid movie ID');
            return;
        }

        console.log('Toggling favorite for:', { userId, movieId, isFavorite });

        try {
            if (isFavorite) {
                console.log('Removing from favorites...');
                await userAPI.removeFromFavorites(userId, movieId);
                setIsFavorite(false);
            } else {
                console.log('Adding to favorites...');
                await userAPI.addToFavorites(userId, movieId);
                setIsFavorite(true);
            }
        } catch (error) {
            console.error('Failed to toggle favorite:', error);
            console.error('Error response data:', error.response?.data);
            alert(`Failed to update favorites: ${error.response?.data?.error || error.message}`);
        }
    };

    const openTrailer = () => {
        if (movie.youtube_link) {
            window.open(movie.youtube_link, '_blank');
        } else {
            // Fallback to YouTube search
            const query = encodeURIComponent(`${movie.title} trailer`);
            window.open(`https://www.youtube.com/results?search_query=${query}`, '_blank');
        }
    };

    const getPosterUrl = (posterPath) => {
        if (!posterPath) return 'https://via.placeholder.com/300x450';
        if (posterPath.startsWith('http')) return posterPath;
        return `https://image.tmdb.org/t/p/w500${posterPath}`;
    };

    if (loading) return <div className="loading">Loading...</div>;
    if (!movie) return <div className="error">Movie not found</div>;

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#0a0a0a' }}>
            <Navbar />
            <div className="movie-details-page">
                <div className="movie-header">
                    <img src={getPosterUrl(movie.poster_path)} alt={movie.title} className="poster" />
                    <div className="movie-info">
                        <h1>{movie.title}</h1>
                        <p className="release-year">Release Year: {movie.release_date}</p>
                        <p className="genres">Genres: {movie.genres?.join(', ') || 'N/A'}</p>
                        <p className="imdb-rating">IMDb Rating: <span>{movie.rating || 'N/A'}</span></p>
                        <p className="user-rating">Our Users Rate: <span>{movie.averageRating ? movie.averageRating.toFixed(1) : '0.0'}</span></p>
                        <p className="overview">{movie.overview}</p>

                        <div className="action-buttons">
                            <button onClick={openIMDb} disabled={!movie.web_link}>Visit IMDb</button>
                            <button onClick={openTrailer}>Watch Trailer</button>
                            <button onClick={() => setShowRatingModal(true)}>Add Rating</button>
                            <button onClick={() => setShowAddToListModal(true)}>Add to List</button>
                            <button onClick={handleToggleFavorite} style={{ fontSize: '1.5rem', padding: '0 10px', display: 'flex', alignItems: 'center', marginLeft: 'auto' }} title={isFavorite ? "Remove from Favorites" : "Add to Favorites"}>
                                {isFavorite ? '❤️' : '🤍'}
                            </button>
                        </div>
                    </div>
                </div>

                <div className="comments-section">
                    <h2>Comments</h2>
                    <form className="comment-form" onSubmit={(e) => e.preventDefault()}>
                        <textarea
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            placeholder="Add your comment..."
                            rows="3"
                        />
                        <button type="button" onClick={handleAddComment}>Post Comment</button>
                    </form>

                    <div className="comments-list">
                        {comments.map(c => (
                            <div key={c.comment_id} className="comment">
                                <strong>{c.firstname} {c.lastname}</strong>
                                <p>{c.comment_text}</p>
                                {user && c.user_id === user.user_id && (
                                    <button onClick={() => handleDeleteComment(c.comment_id)} className="delete-btn">Delete</button>
                                )}
                            </div>
                        ))}
                    </div>
                </div>

                {/* Rating Modal */}
                {showRatingModal && (
                    <div className="modal-overlay" onClick={() => setShowRatingModal(false)}>
                        <div className="modal" onClick={e => e.stopPropagation()}>
                            <h2>Rate this movie (1-10)</h2>
                            <form onSubmit={handleRating}>
                                <input
                                    type="range"
                                    min="1"
                                    max="10"
                                    value={rating}
                                    onChange={(e) => setRating(e.target.value)}
                                />
                                <p>Your rating: {rating}</p>
                                <button type="submit">Submit</button>
                                <button type="button" onClick={() => setShowRatingModal(false)}>Cancel</button>
                            </form>
                        </div>
                    </div>
                )}

                {/* Add to List Modal */}
                {showAddToListModal && (
                    <div className="modal-overlay" onClick={() => setShowAddToListModal(false)}>
                        <div className="modal" onClick={e => e.stopPropagation()}>
                            <h2>Add to List</h2>
                            <div className="lists-selection">
                                {userLists.map(list => (
                                    <button key={list.list_id} onClick={() => handleAddToList(list.list_id)}>
                                        {list.list_name}
                                    </button>
                                ))}
                            </div>
                            <button onClick={() => setShowAddToListModal(false)}>Cancel</button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default MovieDetails;
