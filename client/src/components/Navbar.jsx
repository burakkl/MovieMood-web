import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from '../styles/navbar.module.css';

function Navbar() {
    const { currentUser, logout } = useAuth();
    const location = useLocation();

    const isActive = (path) => location.pathname === path;

    return (
        <nav className={styles.navbar}>
            <div className={styles.logo}>Movie Mood</div>

            <div className={styles.navLinks}>
                <Link
                    to="/home"
                    className={`${styles.navLink} ${isActive('/home') ? styles.active : ''}`}
                >
                    Home
                </Link>

                <Link
                    to="/my-lists"
                    className={`${styles.navLink} ${isActive('/my-lists') ? styles.active : ''}`}
                >
                    My Lists
                </Link>
                <Link
                    to="/movies"
                    className={`${styles.navLink} ${isActive('/movies') ? styles.active : ''}`}
                >
                    Movies
                </Link>
                <Link
                    to="/profile"
                    className={`${styles.navLink} ${isActive('/profile') ? styles.active : ''}`}
                >
                    My Profile
                </Link>
                <Link
                    to="/chat"
                    className={`${styles.navLink} ${isActive('/chat') ? styles.active : ''}`}
                >
                    Chat
                </Link>
            </div>

            <div className={styles.userSection}>
                <span className={styles.userName}>{currentUser?.firstname}</span>
                <button onClick={logout} className={styles.logoutButton}>
                    Logout
                </button>
            </div>
        </nav>
    );
}

export default Navbar;
