import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function ProtectedRoute({ children }) {
    const { currentUser, loading } = useAuth();

    if (loading) {
        return <div className="loading">Loading...</div>;
    }

    if (!currentUser) {
        return <Navigate to="/" replace />;
    }

    return children;
}

export default ProtectedRoute;
