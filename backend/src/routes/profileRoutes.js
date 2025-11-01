// backend/src/routes/profileRoutes.js

const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const protect = require('../middleware/authMiddleware');
const Profile = require('../models/Profile');
const admin = require('../config/firebaseAdmin'); // Used for first-time profile check

// Ensure uploads directory exists
const uploadsDir = path.join(__dirname, '../../uploads/profile_pictures');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure multer for file uploads
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, uploadsDir);
    },
    filename: (req, file, cb) => {
        // Use userId from the authenticated user + timestamp + extension
        const userId = req.user?.id || 'unknown';
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        const ext = path.extname(file.originalname);
        cb(null, `${userId}-${uniqueSuffix}${ext}`);
    }
});

const fileFilter = (req, file, cb) => {
    // Accept only image files
    if (file.mimetype.startsWith('image/')) {
        cb(null, true);
    } else {
        cb(new Error('Only image files are allowed!'), false);
    }
};

const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    }
});

// --- Utility function to calculate Win Rate ---
const calculateWinRate = (wins, losses) => {
    if (wins + losses === 0) return 0;
    return (wins / (wins + losses)) * 100;
};

// @route   GET /api/profile
// @desc    Get the current authenticated user's profile
// @access  Private (Requires JWT)
router.get('/', protect, async (req, res) => {
    try {
        const profile = await Profile.findOne({ userId: req.user.id });

        if (!profile) {
            // If profile doesn't exist, create an initial one
            const newProfile = await Profile.create({ userId: req.user.id });
            return res.status(201).json(newProfile);
        }

        res.json(profile);
    } catch (error) {
        res.status(500).json({ message: 'Server error retrieving profile.' });
    }
});

// @route   PUT /api/profile/customize
// @desc    Update user nickname or profile picture URL
// @access  Private (Requires JWT)
router.put('/customize', protect, async (req, res) => {
    const { nickname, profile_picture_url } = req.body;
    try {
        const updateFields = {};
        if (nickname) updateFields.nickname = nickname;
        if (profile_picture_url) updateFields.profile_picture_url = profile_picture_url;

        const profile = await Profile.findOneAndUpdate(
            { userId: req.user.id },
            { $set: updateFields },
            { new: true, runValidators: true }
        );

        if (!profile) return res.status(404).json({ message: 'Profile not found.' });

        res.json(profile);
    } catch (error) {
        // Handle MongoDB unique key error (e.g., nickname already taken)
        if (error.code === 11000) { 
            return res.status(400).json({ message: 'Nickname is already taken.' });
        }
        res.status(500).json({ message: 'Server error during customization update.' });
    }
});

// @route   POST /api/profile/upload-image
// @desc    Upload profile picture image file
// @access  Private (Requires JWT)
router.post('/upload-image', protect, upload.single('image'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ message: 'No image file provided.' });
        }

        // Construct the URL for the uploaded image
        // This assumes your server is accessible at a base URL
        const baseUrl = process.env.BASE_URL || `http://localhost:${process.env.PORT || 3000}`;
        // The static route /uploads serves from uploads/profile_pictures, so URL is /uploads/filename
        const imageUrl = `${baseUrl}/uploads/${req.file.filename}`;

        res.json({
            success: true,
            imageUrl: imageUrl,
            filename: req.file.filename
        });
    } catch (error) {
        console.error('Error uploading image:', error);
        res.status(500).json({ message: 'Server error uploading image.' });
    }
});

// @route   POST /api/profile/game_result
// @desc    Update score, wins, losses, winrate, and history after a game
// @access  Private (Requires JWT)
router.post('/game_result', protect, async (req, res) => {
    const { didWin, scoreChange, opponentId } = req.body; 

    // Basic Validation
    if (typeof didWin !== 'boolean' || !scoreChange || !opponentId) {
        return res.status(400).json({ message: 'Missing required game result fields.' });
    }

    try {
        const profile = await Profile.findOne({ userId: req.user.id });
        if (!profile) return res.status(404).json({ message: 'Profile not found.' });

        // 1. Update Core Stats
        let newWins = profile.wins;
        let newLosses = profile.losses;
        
        if (didWin) {
            newWins += 1;
        } else {
            newLosses += 1;
        }

        // 2. Calculate New Win Rate
        const newWinRate = calculateWinRate(newWins, newLosses);

        // 3. Prepare Updates
        const updates = {
            $inc: { score: scoreChange }, // $inc increments the value
            wins: newWins,
            losses: newLosses,
            winrate: newWinRate,
            // $push adds a new item to the history_of_games array
            $push: {
                history_of_games: {
                    opponentId,
                    result: didWin ? 'win' : 'loss', 
                    scoreChange 
                }
            }
        };

        const updatedProfile = await Profile.findOneAndUpdate(
            { userId: req.user.id },
            updates,
            { new: true }
        );

        res.json(updatedProfile);

    } catch (error) {
        res.status(500).json({ message: 'Server error processing game result.' });
    }
});

module.exports = router;
