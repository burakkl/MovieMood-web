import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import styles from '../styles/profile.module.css';
import { uploadAPI, userAPI, listAPI, friendAPI } from '../api/client';
import { useRef, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

function Profile() {
    const { currentUser, refreshUser } = useAuth();
    const { id } = useParams();
    const navigate = useNavigate();
    const fileInputRef = useRef(null);

    const [profileUser, setProfileUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Data for the profile being viewed
    const [userLists, setUserLists] = useState([]);
    const [userFriends, setUserFriends] = useState([]);
    const [userFavorites, setUserFavorites] = useState([]);

    // Interactions
    const [friendshipStatus, setFriendshipStatus] = useState('none'); // 'none', 'friends', 'pending_sent', 'pending_received'
    const [requestId, setRequestId] = useState(null);
    const [friendRequests, setFriendRequests] = useState([]); // Only for own profile

    const isOwnProfile = !id || (currentUser && id === String(currentUser.id));

    useEffect(() => {
        if (!currentUser) return;
        fetchProfileData();
    }, [id, currentUser]);

    const fetchProfileData = async () => {
        setLoading(true);
        setError(null);
        try {
            let user;

            if (isOwnProfile) {
                user = currentUser;
                // Fetch friend requests for own profile
                const requestsRes = await friendAPI.getPendingRequests(currentUser.id);
                setFriendRequests(requestsRes.data);
            } else {
                // Fetch other user
                const userRes = await userAPI.getUser(id);
                user = userRes.data;

                // Check friendship status
                const statusRes = await friendAPI.checkStatus(currentUser.id, id);
                setFriendshipStatus(statusRes.data.status);
                setRequestId(statusRes.data.requestId);
            }

            setProfileUser(user);

            // Fetch generic profile data (Lists, Friends, Favorites)
            const userIdToFetch = user.id || user.user_id;

            const [listsRes, friendsRes, favoritesRes] = await Promise.all([
                listAPI.getUserLists(userIdToFetch),
                friendAPI.getFriends(userIdToFetch),
                userAPI.getFavorites(userIdToFetch)
            ]);

            setUserLists(listsRes.data);
            setUserFriends(friendsRes.data);
            setUserFavorites(favoritesRes.data);

        } catch (err) {
            console.error('Failed to fetch profile data:', err);
            setError('Failed to load profile');
        } finally {
            setLoading(false);
        }
    };

    const handleProfilePictureClick = () => {
        if (isOwnProfile) {
            fileInputRef.current.click();
        }
    };

    const handleFileChange = async (event) => {
        const file = event.target.files[0];
        if (!file) return;

        const userId = currentUser.id || currentUser.userId || currentUser.user_id;
        const formData = new FormData();
        formData.append('profilePicture', file);
        formData.append('userId', userId);

        try {
            await uploadAPI.uploadProfilePicture(formData);
            await refreshUser();
        } catch (error) {
            console.error('Failed to upload profile picture:', error);
            alert('Failed to upload profile picture');
        }
    };

    // Friend Actions
    const handleAddFriend = async () => {
        try {
            await friendAPI.sendFriendRequest(currentUser.id, profileUser.user_id);
            setFriendshipStatus('pending_sent');
            // Re-check to get request ID if needed, or just assume sent
            const statusRes = await friendAPI.checkStatus(currentUser.id, profileUser.user_id);
            setRequestId(statusRes.data.requestId);
        } catch (err) {
            alert('Failed to send friend request');
        }
    };

    const handleCancelRequest = async () => {
        if (!requestId) return;
        try {
            await friendAPI.cancelRequest(requestId);
            setFriendshipStatus('none');
            setRequestId(null);
        } catch (err) {
            alert('Failed to cancel request');
        }
    };

    const handleAcceptRequest = async (reqId) => {
        try {
            await friendAPI.acceptRequest(reqId);
            if (isOwnProfile) {
                fetchProfileData(); // Refresh everything
            } else {
                setFriendshipStatus('friends');
                setRequestId(null);
                // Update local friends count/list
                fetchProfileData();
            }
        } catch (err) {
            alert('Failed to accept request');
        }
    };

    const handleRejectRequest = async (reqId) => {
        try {
            await friendAPI.rejectRequest(reqId);
            if (isOwnProfile) {
                fetchProfileData();
            } else {
                setFriendshipStatus('none');
                setRequestId(null);
            }
        } catch (err) {
            alert('Failed to reject request');
        }
    };

    const handleRemoveFriend = async () => {
        if (!confirm('Are you sure you want to remove this friend?')) return;
        try {
            await friendAPI.removeFriend(currentUser.id, profileUser.user_id);
            setFriendshipStatus('none');
            fetchProfileData();
        } catch (err) {
            alert('Failed to remove friend');
        }
    };

    if (loading) return <div className={styles.loading}>Loading...</div>;
    if (error) return <div className={styles.error}>{error}</div>;
    if (!profileUser) return <div className={styles.error}>User not found</div>;

    const profilePictureUrl = profileUser.profilePicture || profileUser.profile_picture_path
        ? ((profileUser.profilePicture || profileUser.profile_picture_path).startsWith('http')
            ? (profileUser.profilePicture || profileUser.profile_picture_path)
            : `http://localhost:3000/${profileUser.profilePicture || profileUser.profile_picture_path}?t=${new Date().getTime()}`)
        : null;

    return (
        <div className={styles.container}>
            <Navbar />

            <div className={styles.content}>
                <div className={styles.profileCard}>
                    <div className={styles.profileHeader}>
                        <div
                            className={styles.profilePicture}
                            onClick={handleProfilePictureClick}
                            style={{ cursor: isOwnProfile ? 'pointer' : 'default' }}
                            title={isOwnProfile ? "Click to change profile picture" : ""}
                        >
                            {isOwnProfile && (
                                <input
                                    type="file"
                                    ref={fileInputRef}
                                    style={{ display: 'none' }}
                                    onChange={handleFileChange}
                                    accept="image/*"
                                />
                            )}
                            {profilePictureUrl ? (
                                <img src={profilePictureUrl} alt="Profile" />
                            ) : (
                                <div className={styles.placeholder}>
                                    {profileUser.firstname?.[0]}{profileUser.lastname?.[0]}
                                </div>
                            )}
                            {isOwnProfile && (
                                <div className={styles.overlay}>
                                    <span>Change</span>
                                </div>
                            )}
                        </div>
                        <div className={styles.profileInfo}>
                            <h1 className={styles.name}>
                                {profileUser.firstname} {profileUser.lastname}
                            </h1>
                            <p className={styles.userId}>ID: {profileUser.userCode || profileUser.user_code || 'N/A'}</p>
                            <p className={styles.email}>{profileUser.email}</p>

                            {!isOwnProfile && (
                                <div className={styles.actions}>
                                    {friendshipStatus === 'none' && (
                                        <button className={styles.actionBtn} onClick={handleAddFriend}>Add Friend</button>
                                    )}
                                    {friendshipStatus === 'pending_sent' && (
                                        <button className={`${styles.actionBtn} ${styles.secondary}`} onClick={handleCancelRequest}>Request Sent (Cancel)</button>
                                    )}
                                    {friendshipStatus === 'pending_received' && (
                                        <div className={styles.actionGroup}>
                                            <button className={`${styles.actionBtn} ${styles.success}`} onClick={() => handleAcceptRequest(requestId)}>Accept Request</button>
                                            <button className={`${styles.actionBtn} ${styles.danger}`} onClick={() => handleRejectRequest(requestId)}>Reject</button>
                                        </div>
                                    )}
                                    {friendshipStatus === 'friends' && (
                                        <button className={`${styles.actionBtn} ${styles.danger}`} onClick={handleRemoveFriend}>Remove Friend</button>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className={styles.sections}>
                        {isOwnProfile && friendRequests.length > 0 && (
                            <div className={styles.section}>
                                <h2 className={styles.sectionTitle}>Friend Requests</h2>
                                <div className={styles.requestList}>
                                    {friendRequests.map(req => (
                                        <div key={req.request_id} className={styles.requestItem}>
                                            <div className={styles.requestInfo}>
                                                <span>{req.firstname} {req.lastname}</span>
                                            </div>
                                            <div className={styles.requestActions}>
                                                <button onClick={() => handleAcceptRequest(req.request_id)} className={styles.acceptBtn}>Accept</button>
                                                <button onClick={() => handleRejectRequest(req.request_id)} className={styles.rejectBtn}>Reject</button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>Lists</h2>
                            {userLists.length > 0 ? (
                                <ul className={styles.list}>
                                    {userLists.map(list => (
                                        <li key={list.list_id}>{list.list_name} ({list.movie_count || 0} movies)</li>
                                    ))}
                                </ul>
                            ) : (
                                <p className={styles.placeholder}>No lists yet</p>
                            )}
                        </div>

                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>Friends ({userFriends.length})</h2>
                            {userFriends.length > 0 ? (
                                <div className={styles.friendGrid}>
                                    {userFriends.map(friend => (
                                        <div key={friend.user_id} className={styles.friendItem} onClick={() => navigate(`/profile/${friend.user_id}`)} style={{ cursor: 'pointer' }}>
                                            <div className={styles.friendAvatar}>
                                                {friend.profile_picture_path ? (
                                                    <img src={friend.profile_picture_path.startsWith('http') ? friend.profile_picture_path : `http://localhost:3000/${friend.profile_picture_path}`} alt="friend" />
                                                ) : (
                                                    <span>{friend.firstname?.[0]}</span>
                                                )}
                                            </div>
                                            <span>{friend.firstname} {friend.lastname}</span>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className={styles.placeholder}>No friends yet</p>
                            )}
                        </div>

                        <div className={styles.section}>
                            <h2 className={styles.sectionTitle}>Favorites ({userFavorites.length})</h2>
                            {userFavorites.length > 0 ? (
                                <div className={styles.movieGrid}>
                                    {userFavorites.map(movie => (
                                        <div key={movie.movie_id} className={styles.miniMovie} onClick={() => navigate(`/movies/${movie.movie_id}`)}>
                                            <img src={movie.poster_url} alt={movie.title} />
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className={styles.placeholder}>No favorites yet</p>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Profile;
