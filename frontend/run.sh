# Clear Port 5173
kill -9 $(lsof -ti :5173)

# Start App
npm run dev