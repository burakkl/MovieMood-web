import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { io } from 'socket.io-client';
import { useAuth } from '../context/AuthContext';
import { chatAPI, friendAPI } from '../api/client';
import Navbar from '../components/Navbar';
import './Chat.css';

function Chat() {
    const [friends, setFriends] = useState([]);
    const [selectedFriend, setSelectedFriend] = useState(null);
    const [messages, setMessages] = useState([]);
    const [messageText, setMessageText] = useState('');
    const [socket, setSocket] = useState(null);
    const [chatId, setChatId] = useState(null);
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
        const newSocket = io('http://localhost:3000');
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
                                        <p>{msg.message_text}</p>
                                        <span className="timestamp">
                                            {new Date(msg.created_at).toLocaleTimeString()}
                                        </span>
                                    </div>
                                ))}
                            </div>
                            <form onSubmit={sendMessage} className="message-input-form">
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
        </div>
    );
}

export default Chat;
