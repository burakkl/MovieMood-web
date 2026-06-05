import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { io } from 'socket.io-client';
import { useAuth } from '../context/AuthContext';
import { chatAPI, friendAPI, listAPI } from '../api/client';
import Navbar from '../components/Navbar';
import './Chat.css';

function Chat() {
    const [friends, setFriends] = useState([]);
    const [selectedFriend, setSelectedFriend] = useState(null);
    const [messages, setMessages] = useState([]);
    const [messageText, setMessageText] = useState('');
    const [socket, setSocket] = useState(null);
    const [chatId, setChatId] = useState(null);
    const [showShareModal, setShowShareModal] = useState(false);
    const [myLists, setMyLists] = useState([]);
    const [viewListModal, setViewListModal] = useState(null);
    const { currentUser: user } = useAuth();
    const navigate = useNavigate();
    const userId = user?.userId || user?.user_id;

    useEffect(() => {
        if (!userId) {
            navigate('/login');
            return;
        }

        loadFriends();
        setupWebSocket();

        return () => {
            if (socket) socket.disconnect();
        };
    }, [userId]);

    const setupWebSocket = () => {
        const socketUrl = import.meta.env.DEV ? 'http://localhost:3000' : window.location.origin;
        const newSocket = io(socketUrl);
        newSocket.on('connect', () => {
            newSocket.emit('register', userId);
        });

        newSocket.on('message_received', (message) => {
            setMessages(prev => [...prev, message]);
        });

        setSocket(newSocket);
    };

    const loadFriends = async () => {
        try {
            const response = await friendAPI.getFriends(userId);
            setFriends(response.data);
        } catch (error) {
            console.error('Failed to load friends:', error);
        }
    };

    const selectFriend = async (friend) => {
        setSelectedFriend(friend);
        try {
            // Get or create chat
            const chatResponse = await chatAPI.getOrCreateChat(userId, friend.user_id);
            setChatId(chatResponse.data.chat_id);

            // Load messages
            const messagesResponse = await chatAPI.getChatMessages(chatResponse.data.chat_id);
            setMessages(messagesResponse.data);
        } catch (error) {
            console.error('Failed to load chat:', error);
        }
    };

    const sendMessage = (e) => {
        e.preventDefault();
        if (!messageText.trim() || !socket || !chatId || !selectedFriend) return;

        socket.emit('send_message', {
            chatId,
            senderId: userId,
            receiverId: selectedFriend.user_id,
            messageText
        });

        setMessageText('');
    };

    const handleOpenShareModal = async () => {
        try {
            const response = await listAPI.getUserLists(userId);
            setMyLists(response.data);
            setShowShareModal(true);
        } catch (error) {
            console.error('Failed to load lists for sharing:', error);
        }
    };

    const shareList = (list) => {
        if (!socket || !chatId || !selectedFriend) return;
        const text = `[SHARED_LIST:${list.list_id}:${list.list_name}]`;
        socket.emit('send_message', {
            chatId,
            senderId: userId,
            receiverId: selectedFriend.user_id,
            messageText: text
        });
        setShowShareModal(false);
    };

    const openSharedList = async (listId, listOwnerId) => {
        try {
            const res = await listAPI.getUserLists(listOwnerId);
            const list = res.data.find(l => String(l.list_id) === String(listId));
            if (list) {
                setViewListModal(list);
            } else {
                alert('List not found or deleted');
            }
        } catch (e) {
            console.error('Failed to load shared list', e);
        }
    };

    const renderMessageContent = (msg) => {
        const match = msg.message_text.match(/^\[SHARED_LIST:([^:]+):(.+)\]$/);
        if (match) {
            const listId = match[1];
            const listName = match[2];
            return (
                <div className="shared-list-card" onClick={() => openSharedList(listId, msg.sender_id)}>
                    <div className="shared-list-icon">🎬</div>
                    <div className="shared-list-info">
                        <strong>Shared List: {listName}</strong>
                        <span>Click to view movies</span>
                    </div>
                </div>
            );
        }
        return <p>{msg.message_text}</p>;
    };

    const getPosterUrl = (posterPath) => {
        if (!posterPath) return 'https://via.placeholder.com/150x225';
        if (posterPath.startsWith('http')) return posterPath;
        return `https://image.tmdb.org/t/p/w500${posterPath}`;
    };

    return (
        <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Navbar />
            <div className="chat-page" style={{ flex: 1, height: 'auto' }}>
                <div className="chat-sidebar">
                    <h2>Friends</h2>
                    <div className="friends-list">
                        {friends.map(friend => (
                            <div
                                key={friend.user_id}
                                className={`friend-item ${selectedFriend?.user_id === friend.user_id ? 'active' : ''}`}
                                onClick={() => selectFriend(friend)}
                            >
                                <img
                                    src={friend.profile_picture_path || 'https://via.placeholder.com/50'}
                                    alt={friend.firstname}
                                />
                                <div className="friend-info">
                                    <strong>{friend.firstname} {friend.lastname}</strong>
                                </div>
                            </div>
                        ))}
                        {friends.length === 0 && (
                            <p className="no-friends">No friends yet. Add friends from your profile!</p>
                        )}
                    </div>
                </div>

                <div className="chat-main">
                    {selectedFriend ? (
                        <>
                            <div className="chat-header">
                                <h2>{selectedFriend.firstname} {selectedFriend.lastname}</h2>
                            </div>
                            <div className="messages-container">
                                {messages.map((msg, idx) => (
                                    <div
                                        key={idx}
                                        className={`message ${msg.sender_id === userId ? 'sent' : 'received'}`}
                                    >
                                        {renderMessageContent(msg)}
                                        <span className="timestamp">
                                            {new Date(msg.created_at).toLocaleTimeString()}
                                        </span>
                                    </div>
                                ))}
                            </div>
                            <form onSubmit={sendMessage} className="message-input-form">
                                <button type="button" className="share-list-btn" onClick={handleOpenShareModal}>
                                    🔗 Share List
                                </button>
                                <input
                                    type="text"
                                    value={messageText}
                                    onChange={(e) => setMessageText(e.target.value)}
                                    placeholder="Type a message..."
                                />
                                <button type="submit">Send</button>
                            </form>
                        </>
                    ) : (
                        <div className="no-chat-selected">
                            <p>Select a friend to start chatting</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Share List Modal */}
            {showShareModal && (
                <div className="chat-modal-overlay" onClick={() => setShowShareModal(false)}>
                    <div className="chat-modal-content" onClick={e => e.stopPropagation()}>
                        <h2>Share a List</h2>
                        <div className="modal-lists-container">
                            {myLists.length > 0 ? (
                                myLists.map(list => (
                                    <div key={list.list_id} className="modal-list-item" onClick={() => shareList(list)}>
                                        <strong>{list.list_name}</strong>
                                        <span>({list.movies?.length || 0} movies)</span>
                                    </div>
                                ))
                            ) : (
                                <p>You don't have any lists yet.</p>
                            )}
                        </div>
                        <button className="chat-modal-close" onClick={() => setShowShareModal(false)}>Close</button>
                    </div>
                </div>
            )}

            {/* View Shared List Modal */}
            {viewListModal && (
                <div className="chat-modal-overlay" onClick={() => setViewListModal(null)}>
                    <div className="chat-modal-content large" onClick={e => e.stopPropagation()}>
                        <h2>Shared List: {viewListModal.list_name}</h2>
                        <div className="shared-movies-grid">
                            {viewListModal.movies && viewListModal.movies.length > 0 ? (
                                viewListModal.movies.map(movie => (
                                    <div key={movie.movie_id} className="shared-movie-card" onClick={() => navigate(`/movies/${movie.movie_id}`)}>
                                        <img src={getPosterUrl(movie.poster_path)} alt={movie.title} />
                                        <p>{movie.title}</p>
                                    </div>
                                ))
                            ) : (
                                <p>This list is empty.</p>
                            )}
                        </div>
                        <button className="chat-modal-close" onClick={() => setViewListModal(null)}>Close</button>
                    </div>
                </div>
            )}
        </div>
    );
}

export default Chat;
