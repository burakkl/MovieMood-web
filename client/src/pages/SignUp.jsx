import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from '../styles/login.module.css';

function SignUp() {
    const [email, setEmail] = useState('');
    const [firstname, setFirstname] = useState('');
    const [lastname, setLastname] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();
    const { register } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        const result = await register(email, firstname, lastname, password);
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
                    <p className={styles.subtitle}>Create your account</p>

                    <form onSubmit={handleSubmit} className={styles.form}>
                        <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className={styles.input}
                            required
                        />
                        <input
                            type="text"
                            placeholder="First Name"
                            value={firstname}
                            onChange={(e) => setFirstname(e.target.value)}
                            className={styles.input}
                            required
                        />
                        <input
                            type="text"
                            placeholder="Last Name"
                            value={lastname}
                            onChange={(e) => setLastname(e.target.value)}
                            className={styles.input}
                            required
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className={styles.input}
                            required
                        />

                        {error && <p className={styles.error}>{error}</p>}

                        <button type="submit" className={styles.signInButton}>
                            Sign Up
                        </button>
                    </form>
                </div>
            </div>

            <div className={styles.rightPanel}>
                <div className={styles.signUpSection}>
                    <h2 className={styles.question}>ALREADY HAVE AN ACCOUNT?</h2>
                    <button
                        onClick={() => navigate('/')}
                        className={styles.signUpButton}
                    >
                        Sign In
                    </button>
                </div>
            </div>
        </div>
    );
}

export default SignUp;
