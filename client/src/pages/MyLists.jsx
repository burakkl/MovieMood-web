import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { listAPI, movieAPI, recommendationAPI, userAPI } from '../api/client';
import './MyLists.css';

function MyLists() {
    const [lists, setLists] = useState([]);
    const [favorites, setFavorites] = useState([]);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newListName, setNewListName] = useState('');
    const [showManageModal, setShowManageModal] = useState(false);
    const [selectedList, setSelectedList] = useState(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [editingListName, setEditingListName] = useState('');
    const [isEditingName, setIsEditingName] = useState(false);
    const navigate = useNavigate();
    const { currentUser: user } = useAuth();
    const userId = user?.userId || user?.user_id;

    useEffect(() => {
        if (userId) {
            loadLists();
        } else {
            setLists([]);
        }
    }, [userId]);

    const loadLists = async () => {
        try {
            const [listsRes, favRes] = await Promise.all([
                listAPI.getUserLists(userId),
                userAPI.getFavorites(userId)
            ]);

            const favList = {
                list_id: 'favorites',
                list_name: 'Favorites',
                movies: favRes.data,
                isFavorites: true
            };

            setLists([favList, ...listsRes.data]);
        } catch (error) {
            console.error('Failed to load lists:', error);
        }
    };

    const createList = async (e) => {
        if (e) e.preventDefault();
        console.log('Creating list:', newListName);
        console.log('User ID from localStorage:', userId);

        if (!newListName.trim()) {
            console.log('List name is empty');
            return;
        }

        if (!userId) {
            console.error('User ID is missing');
            alert('Error: User ID not found. Please logout and login again.');
            return;
        }

        try {
            await listAPI.createList(userId, newListName);
            setNewListName('');
            setShowCreateModal(false);
            loadLists();
            console.log('List created successfully');
        } catch (error) {
            console.error('Failed to create list:', error);
        }
    };

    const deleteList = async (listId) => {
        if (!confirm('Are you sure you want to delete this list?')) return;

        try {
            await listAPI.deleteList(listId);
            loadLists();
        } catch (error) {
            console.error('Failed to delete list:', error);
        }
    };

    const handleRenameList = async () => {
        if (!editingListName.trim()) return;

        try {
            await listAPI.renameList(selectedList.list_id, editingListName);

            // Update local lists
            const updatedLists = lists.map(l =>
                l.list_id === selectedList.list_id
                    ? { ...l, list_name: editingListName }
                    : l
            );
            setLists(updatedLists);

            // Update selected list
            setSelectedList(prev => ({ ...prev, list_name: editingListName }));
            setIsEditingName(false);
        } catch (error) {
            console.error('Failed to rename list:', error);
            alert('Failed to rename list');
        }
    };

    const openManageModal = (list) => {
        setSelectedList(list);
        setShowManageModal(true);
        setSearchQuery('');
        setSearchResults([]);
        setEditingListName(list.list_name);
        setIsEditingName(false);
    };

    const searchMovies = async (e) => {
        e.preventDefault();
        if (!searchQuery.trim()) return;

        try {
            const response = await movieAPI.search(searchQuery);
            setSearchResults(response.data);
        } catch (error) {
            console.error('Search failed:', error);
        }
    };

    const addMovieToList = async (movieId) => {
        try {
            if (selectedList.list_id === 'favorites') {
                await userAPI.addToFavorites(userId, movieId);
                // Refresh favorites
                const favRes = await userAPI.getFavorites(userId);
                const updatedFavList = {
                    ...selectedList,
                    movies: favRes.data
                };
                setSelectedList(updatedFavList);
                // Update lists state
                setLists(prev => prev.map(l => l.list_id === 'favorites' ? updatedFavList : l));
            } else {
                await listAPI.addMovieToList(selectedList.list_id, movieId);
                loadLists(); // Refresh all lists to get updated counts

                // Refresh selected list
                const updatedLists = await listAPI.getUserLists(userId);
                const updated = updatedLists.data.find(l => l.list_id === selectedList.list_id);
                setSelectedList(updated);
            }
        } catch (error) {
            alert(error.response?.data?.error || 'Failed to add movie');
        }
    };

    const removeMovieFromList = async (movieId) => {
        try {
            if (selectedList.list_id === 'favorites') {
                await userAPI.removeFromFavorites(userId, movieId);
                // Refresh favorites
                const favRes = await userAPI.getFavorites(userId);
                const updatedFavList = {
                    ...selectedList,
                    movies: favRes.data
                };
                setSelectedList(updatedFavList);
                setLists(prev => prev.map(l => l.list_id === 'favorites' ? updatedFavList : l));
            } else {
                await listAPI.removeMovieFromList(selectedList.list_id, movieId);
                loadLists();
                const updatedLists = await listAPI.getUserLists(userId);
                const updated = updatedLists.data.find(l => l.list_id === selectedList.list_id);
                setSelectedList(updated);
            }
        } catch (error) {
            console.error('Failed to remove movie:', error);
        }
    };

    const generateRecommendedList = async () => {
        try {
            const response = await recommendationAPI.generateForUser(userId);
            const { recommendations, metadata } = response.data;

            if (recommendations.length === 0) {
                alert('Not enough data to generate recommendations. Watch some movies first!');
                return;
            }

            if (recommendations.length === 0) {
                alert('Not enough data to generate recommendations. Watch some movies first!');
                return;
            }

            const timestamp = new Date().toLocaleString();
            const listName = `Recommended for You (${timestamp})`;
            const createResponse = await listAPI.createList(userId, listName);

            // Backend returns list_id (snake_case) from database
            const newListId = createResponse.data.list_id || createResponse.data.listId;

            if (!newListId) {
                throw new Error('Failed to get new list ID');
            }

            console.log(`Created list ${newListId}, adding ${recommendations.length} movies...`);

            for (const movie of recommendations) {
                await listAPI.addMovieToList(newListId, movie.movie_id);
            }

            loadLists();
            alert(`Generated a list with ${recommendations.length} recommended movies!\\n\\nBased on: ${metadata.topGenres.join(', ')}`);
        } catch (error) {
            console.error('Recommendation generation failed:', error);
            alert(error.response?.data?.error || 'Failed to generate recommendations');
        }
    };

    const getPosterUrl = (posterPath) => {
        if (!posterPath) return 'https://via.placeholder.com/150x225';
        if (posterPath.startsWith('http')) return posterPath;
        return `https://image.tmdb.org/t/p/w500${posterPath}`;
    };

    return (
        <div style={{ minHeight: '100vh', backgroundColor: '#141414' }}>
            <Navbar />
            <div className="my-lists-page">
                <div className="lists-header">
                    <h1>My Lists</h1>
                    <div className="header-buttons">
                        <button className="create-list-btn" onClick={() => setShowCreateModal(true)}>
                            Create New List
                        </button>
                        <button className="recommend-btn" onClick={generateRecommendedList}>
                            ✨ Generate Recommended List
                        </button>
                    </div>
                </div>

                <div className="lists-container">
                    {lists.map(list => (
                        <div key={list.list_id} className="list-section">
                            <div className="list-header-row">
                                <h2>{list.list_name} <span className="movie-count">({list.movies?.length || 0})</span></h2>
                                {!list.isFavorites && (
                                    <div className="list-actions">
                                        <button className="manage-btn" onClick={() => openManageModal(list)}>Manage List</button>
                                        <button className="delete-btn" onClick={() => deleteList(list.list_id)}>Delete</button>
                                    </div>
                                )}
                            </div>

                            <div className="list-scroll-container">
                                {list.movies && list.movies.length > 0 ? (
                                    <div className="list-movies-row">
                                        {list.movies.map(movie => (
                                            <div
                                                key={movie.movie_id}
                                                className="movie-card-item"
                                                onClick={() => navigate(`/movies/${movie.movie_id}`)}
                                            >
                                                <img
                                                    src={getPosterUrl(movie.poster_path)}
                                                    alt={movie.title}
                                                    className="movie-poster"
                                                />
                                                <div className="movie-overlay">
                                                    <span>{movie.title}</span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="empty-list-placeholder">
                                        <p>No movies in this list yet</p>
                                        <button onClick={() => openManageModal(list)}>+ Add Movies</button>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}

                    {lists.length === 0 && (
                        <div className="no-lists-state">
                            <p>You haven't created any lists yet.</p>
                            <p>Click "Create New List" to get started!</p>
                        </div>
                    )}
                </div>

                {/* Create List Modal */}
                {showCreateModal && (
                    <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
                        <div className="modal-content" onClick={e => e.stopPropagation()}>
                            <h2>Create New List</h2>
                            <form onSubmit={(e) => e.preventDefault()}>
                                <input
                                    type="text"
                                    placeholder="List name..."
                                    value={newListName}
                                    onChange={(e) => setNewListName(e.target.value)}
                                    autoFocus
                                />
                                <div className="modal-actions">
                                    <button type="button" onClick={createList}>Create</button>
                                    <button type="button" onClick={() => setShowCreateModal(false)}>Cancel</button>
                                </div>
                            </form>
                        </div>
                    </div>
                )}

                {/* Manage List Modal */}
                {showManageModal && selectedList && (
                    <div className="modal-overlay" onClick={() => setShowManageModal(false)}>
                        <div className="modal-content large" onClick={e => e.stopPropagation()}>
                            <div className="modal-header">
                                {isEditingName ? (
                                    <div className="rename-container">
                                        <input
                                            type="text"
                                            value={editingListName}
                                            onChange={(e) => setEditingListName(e.target.value)}
                                            className="rename-input"
                                            autoFocus
                                        />
                                        <div className="rename-actions">
                                            <button onClick={handleRenameList} className="save-btn">Save</button>
                                            <button onClick={() => setIsEditingName(false)} className="cancel-btn">Cancel</button>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="title-container">
                                        <h2>{selectedList.list_name}</h2>
                                        <button
                                            onClick={() => setIsEditingName(true)}
                                            className="edit-icon-btn"
                                            title="Rename List"
                                        >
                                            ✏️
                                        </button>
                                    </div>
                                )}
                            </div>

                            <div className="current-movies-section">
                                <h3>Movies in this list</h3>
                                <div className="movies-grid-small">
                                    {selectedList.movies?.map(movie => (
                                        <div key={movie.movie_id} className="manage-movie-card">
                                            <img
                                                src={getPosterUrl(movie.poster_path)}
                                                alt={movie.title}
                                            />
                                            <p>{movie.title}</p>
                                            <button onClick={() => removeMovieFromList(movie.movie_id)}>Remove</button>
                                        </div>
                                    ))}
                                    {(!selectedList.movies || selectedList.movies.length === 0) && (
                                        <p className="empty-state">No movies in this list</p>
                                    )}
                                </div>
                            </div>

                            <div className="add-movies-section">
                                <h3>Add Movies</h3>
                                <form onSubmit={searchMovies} className="search-form">
                                    <input
                                        type="text"
                                        placeholder="Search movies to add..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                    />
                                    <button type="submit">Search</button>
                                </form>
                                <div className="movies-grid-small">
                                    {searchResults.map(movie => {
                                        const isInList = selectedList.movies?.some(m => m.movie_id === movie.movie_id);
                                        return (
                                            <div key={movie.movie_id} className="manage-movie-card">
                                                <img
                                                    src={getPosterUrl(movie.poster_path)}
                                                    alt={movie.title}
                                                />
                                                <p>{movie.title}</p>
                                                <button
                                                    onClick={() => addMovieToList(movie.movie_id)}
                                                    disabled={isInList}
                                                >
                                                    {isInList ? 'In List' : 'Add'}
                                                </button>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>

                            <button className="close-modal-btn" onClick={() => setShowManageModal(false)}>Close</button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default MyLists;
