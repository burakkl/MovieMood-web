import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from '../styles/login.module.css';

function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const { login } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        const result = await login(email, password);
        if (result.success) {
            navigate('/home');
        } else {
            setError(result.error);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.leftPanel}>
                <div className={styles.formContainer}>
                    <h1 className={styles.title}>Movie Mood</h1>
                    <p className={styles.subtitle}>Welcome back!</p>

                    <form onSubmit={handleSubmit} className={styles.form}>
                        <input
                            type="email"
                            placeholder="Enter your email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className={styles.input}
                            required
                        />
                        <input
                            type="password"
                            placeholder="Enter your password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className={styles.input}
                            required
                        />

                        {error && <p className={styles.error}>{error}</p>}

                        <button type="submit" className={styles.signInButton}>
                            Sign In
                        </button>
                    </form>
                </div>
            </div>

            <div className={styles.rightPanel}>
                <div className={styles.signUpSection}>
                    <h2 className={styles.question}>DON'T HAVE AN ACCOUNT?</h2>
                    <button
                        onClick={() => navigate('/signup')}
                        className={styles.signUpButton}
                    >
                        Create Account
                    </button>
                </div>
            </div>
        </div>
    );
}

export default Login;
