# How to Configure the Backend Server

This guide explains how to tell PharmaSmart where to find the backend server.

## Do I need this?

**NO** if:

- ✅ The backend is on the same computer as the app
- ✅ You're using the standalone version (with built-in backend)

**YES** if:

- ❌ The backend is on a **different computer** on your network
- ❌ The backend uses a **different port** than 8080

---

## Simple 3-Step Setup

### Step 1: Find the Backend Computer's Address

On the computer running the backend server:

**Windows:**

1. Press `Windows + R`
2. Type `cmd` and press Enter
3. Type `ipconfig` and press Enter
4. Look for "IPv4 Address" (example: `192.168.1.50`)

**Example:**

```
IPv4 Address. . . . . . . . : 192.168.1.50
```

### Step 2: Create the Config File

1. **Right-click** next to `PharmaSmart.exe`
2. Choose **New → Text Document**
3. Name it exactly: `backend-url.txt` (delete the `.txt.txt` if Windows adds it twice)

### Step 3: Edit the Config File

1. **Right-click** `backend-url.txt`
2. Choose **Open with → Notepad**
3. Type the backend address:
   ```
   http://192.168.1.50:8080
   ```
   (Replace `192.168.1.50` with your backend computer's address from Step 1)
4. **Save** and close Notepad

### Step 4: Launch the App

Double-click `PharmaSmart.exe` - it will now connect to the backend on the other computer!

---

## Visual Guide

```
📁 Your Installation Folder
├── 📄 PharmaSmart.exe          ← Your app
└── 📄 backend-url.txt          ← Config file you create
       │
       └── Contains: http://192.168.1.50:8080
```

---

## Examples

### Example 1: Backend on another computer (IP: 192.168.1.100)

**Edit `backend-url.txt` to contain:**

```
http://192.168.1.100:8080
```

### Example 2: Backend on different port (9090)

**Edit `backend-url.txt` to contain:**

```
http://localhost:9090
```

### Example 3: Backend with a computer name

**Edit `backend-url.txt` to contain:**

```
http://SERVER-PC:8080
```

---

## Testing the Connection

Before launching PharmaSmart, test if you can reach the backend:

1. Open your web browser (Chrome, Firefox, etc.)
2. Type in the address bar:
   ```
   http://192.168.1.50:8080/management/health
   ```
   (Replace with your backend address)
3. You should see some text with "status"

If you see an error, the backend is not reachable - check:

- ✅ Is the backend server running?
- ✅ Is the IP address correct?
- ✅ Are both computers on the same network?

---

## Troubleshooting

### ❌ "Backend not available" message

**Solution 1: Check the backend is running**

- Go to the backend computer
- Make sure the backend application is started

**Solution 2: Check the config file**

- Open `backend-url.txt` with Notepad
- Make sure there are no extra spaces or blank lines
- Make sure it starts with `http://`
- Make sure the IP address is correct

**Solution 3: Check network connection**

- Open Command Prompt (Windows + R, type `cmd`)
- Type: `ping 192.168.1.50` (use your backend IP)
- You should see replies - if not, there's a network problem

**Solution 4: Check Windows Firewall**

- On the **backend computer**, open Windows Firewall
- Allow the backend application through the firewall

### ❌ Config file not working

**Make sure:**

1. File is named exactly `backend-url.txt` (not `backend-url.txt.txt`)
2. File is in the **same folder** as `PharmaSmart.exe`
3. File contains only one line (the URL)
4. No extra spaces before or after the URL

To check the file name:

1. Open the folder with `PharmaSmart.exe`
2. Click **View** menu → Check **File name extensions**
3. You should see `backend-url.txt` (not `backend-url.txt.txt`)

---

## Quick Checklist

- [ ] Backend server is running
- [ ] I know the backend computer's IP address
- [ ] Created `backend-url.txt` next to `PharmaSmart.exe`
- [ ] `backend-url.txt` contains the correct URL
- [ ] I can open `http://[IP]:8080/management/health` in my browser
- [ ] Both computers are on the same network

---

## Still Need Help?

If you're still having trouble, gather this information:

1. What's in your `backend-url.txt` file?
2. Can you ping the backend computer?
3. What error message does PharmaSmart show?

Contact your system administrator with this information.
