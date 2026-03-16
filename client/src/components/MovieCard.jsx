
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from '../styles/home.module.css'; // Reusing home styles for cards

function MovieCard({ movie, isFavorite, onToggleFavorite }) {
    const navigate = useNavigate();

    const getPosterUrl = (posterPath) => {
        if (!posterPath) return 'https://via.placeholder.com/150x225?text=No+Poster';
        if (posterPath.startsWith('http')) return posterPath;
        return `https://image.tmdb.org/t/p/w500${posterPath}`;
    };

    const handleHeartClick = (e) => {
        e.stopPropagation(); // Prevent card click
        onToggleFavorite(movie.movie_id);
    };

    return (
        <div
            className={styles.movieCard}
            onClick={() => navigate(`/movies/${movie.movie_id}`)}
        >
            <div className={styles.posterWrapper} style={{ position: 'relative' }}>
                <img
                    src={getPosterUrl(movie.poster_path)}
                    alt={movie.title}
                    className={styles.poster}
                />
                <div className={styles.overlay}>
                    <button className={styles.playBtn}>▶</button>
                    <button
                        className="favorite-btn"
                        onClick={handleHeartClick}
                        style={{
                            position: 'absolute',
                            bottom: '10px',
                            right: '10px',
                            background: 'rgba(0, 0, 0, 0.5)',
                            border: 'none',
                            borderRadius: '50%',
                            width: '40px',
                            height: '40px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontSize: '24px',
                            cursor: 'pointer',
                            color: isFavorite ? 'red' : 'white',
                            transition: 'all 0.2s',
                            zIndex: 20,
                            padding: '0'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
                        onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                        title={isFavorite ? "Remove from Favorites" : "Add to Favorites"}
                    >
                        {isFavorite ? '❤️' : '🤍'}
                    </button>
                </div>
            </div>
            <div className={styles.movieInfo}>
                <h3 className={styles.movieTitle}>{movie.title}</h3>
                <div className={styles.rating}>⭐ {movie.rating?.toFixed(1) || 'N/A'}</div>
            </div>
        </div>
    );
}

export default MovieCard;
