# MovieMood Web Application

A Netflix-inspired movie discovery and social platform built with React and Node.js. This is a complete rewrite of the Java Swing desktop application into a modern web application.

## 🎬 Features

- **User Authentication**: Register and login with email/password
- **Movie Browsing**: Browse and search through a large movie database
- **Movie Details**: View detailed information including ratings, genres, cast, and trailers
- **Rating System**: Rate movies from 1-10 stars
- **Comments**: Add, view, and manage comments on movies
- **Favorites**: Save movies to your favorites list
- **Responsive Design**: Modern, Netflix-inspired UI with dark theme

## 🛠️ Tech Stack

### Frontend
- React 18
- React Router for navigation
- Axios for API calls
- Vite for build tooling
- CSS Modules for styling

### Backend
- Node.js + Express
- SQLite database (better-sqlite3)
- Express Session for authentication
- CORS enabled for local development

## 🚀 Getting Started

### Prerequisites
- Node.js (v18 or higher)
- npm

### Installation

1. **Install all dependencies**:
```bash
# Install root dependencies
npm install

# Install server dependencies
cd server
npm install

# Install client dependencies
cd ../client
npm install
```

### Running the Application

**Option 1: Run both server and client concurrently (recommended)**
```bash
# From the root directory
npm run dev
```

**Option 2: Run separately**
```bash
# Terminal 1: Start the backend server (port 3000)
cd server
npm run dev

# Terminal 2: Start the React dev server (port 5173)
cd client
npm run dev
```

The application will be available at:
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:3000

## 📁 Project Structure

```
moviemood-web/
├── server/                    # Backend Express API
│   ├── server.js             # Main Express server
│   ├── controllers/          # Database manager
│   └── routes/               # API endpoints (auth, movies, users, friends)
├── client/                    # Frontend React App
│   ├── src/
│   │   ├── pages/            # Page components (Login, Home, MovieDetails, etc.)
│   │   ├── components/       # Reusable components (Navbar, etc.)
│   │   ├── context/          # React context (AuthContext)
│   │   ├── api/              # API client (Axios)
│   │   └── styles/           # CSS modules
│   └── vite.config.js        # Vite configuration
└── moviemood.db              # SQLite database
```

## 🎨 Design

The application features a Netflix-inspired design with:
- Dark color scheme (#141414 background)
- Netflix red (#E50914) accent color
- Smooth transitions and hover effects
- Responsive grid layouts
- Modern typography

## 🔑 Key Features Explained

### Authentication
- Register new users with email, first name, last name, and password
- Login with email and password
- Session-based authentication
- Auto-login from localStorage

### Movies
- Browse popular movies with poster images
- Search movies by title
- View detailed movie information
- Rate movies (1-10 stars)
- Add and view comments
- TMDB API integration for poster images

### User Profile
- View user information
- Display user ID, name, and email
- Sections for favorites, friends, and lists (to be implemented)

## 📝 API Endpoints

### Auth
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Get current user

### Movies
- `GET /api/movies` - Get all movies (with pagination)
- `GET /api/movies/:id` - Get movie by ID
- `GET /api/movies/search?q=query` - Search movies
- `POST /api/movies/:id/comments` - Add comment
- `POST /api/movies/:id/rate` - Rate movie

### Users
- `GET /api/users/:id` - Get user profile
- `GET /api/users/:id/favorites` - Get user's favorites
- `POST /api/users/:id/favorites` - Add to favorites
- `DELETE /api/users/:id/favorites/:movieId` - Remove from favorites

## 🔧 Development

### Running Tests
```bash
# Backend tests (if available)
cd server
npm test

# Frontend tests (if available)
cd client
npm test
```

### Building for Production
```bash
# Build the React app
cd client
npm run build
```

## 📄 License

This project was converted from a Java Swing desktop application to a modern web application.

## 🤝 Contributing

Feel free to fork this project and submit pull requests!

## 🐛 Known Issues

- Database comes empty by default - you'll need to add movies manually or integrate with TMDB API
- Friend system and chat features are partially implemented
- Some profile features are placeholder only

## 🚧 Future Enhancements

- [ ] Complete friend request system
- [ ] Implement chat functionality
- [ ] Add movie lists management
- [ ] Recently watched tracking
- [ ] Movie recommendations
- [ ] Genre-based browsing
- [ ] Advanced search filters
- [ ] User profile picture upload
- [ ] Admin panel for movie management

---

**Enjoy exploring movies with MovieMood! 🎬🍿**
