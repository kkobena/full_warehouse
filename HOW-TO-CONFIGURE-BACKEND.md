# How to Configure the Backend Server

This guide explains how to configure PharmaSmart's backend server.

## Two Configuration Types

### Type 1: Standalone Version (Built-in Backend)

Use `config.json` to configure the **built-in backend** (port, logs, etc.)

**When to use:** You have the standalone Tauri app with bundled backend

👉 **See: [Standalone Configuration](#standalone-configuration-configjson)** below

### Type 2: Remote Backend

Use `backend-url.txt` to point the frontend to a **remote backend server**

**When to use:**
- ❌ The backend is on a **different computer** on your network
- ❌ You want to use a centralized backend

👉 **See: [Remote Backend Configuration](#remote-backend-configuration-backend-urltxt)** below

---

## Standalone Configuration (config.json)

### What is this for?

Configure the **bundled backend** in your standalone PharmaSmart installation:
- Change the server port (default: 9080)
- Customize log file location
- Adjust installation paths
- **Customize JVM memory and performance settings**

### Where is the file?

```
📁 Your Installation Folder
├── 📄 PharmaSmart.exe          ← Your app
├── 📄 config.json              ← Backend configuration
└── 📁 logs/                    ← Backend logs
```

The `config.json` file is **automatically created** next to `PharmaSmart.exe` on first launch.

### Configuration Options

Open `config.json` with Notepad to see:

```json
{
  "server": {
    "port": 9080
  },
  "logging": {
    "directory": "./logs",
    "file": "./logs/pharmasmart.log"
  },
  "installation": {
    "directory": ""
  },
  "jvm": {
    "heap_min": "512m",
    "heap_max": "1g",
    "metaspace_size": "128m",
    "metaspace_max": "256m",
    "direct_memory_size": "256m",
    "max_gc_pause_millis": "200",
    "additional_options": []
  }
}
```

#### Change the Port

**Example:** Change from 9080 to 8080:

1. **Right-click** `config.json` → **Open with** → **Notepad**
2. Change `"port": 9080` to `"port": 8080`
3. **Save** and close Notepad
4. **Restart** PharmaSmart

#### Change Log Location

**Example:** Save logs to a different folder:

1. Change `"directory": "./logs"` to `"directory": "C:/PharmaSmart/Logs"`
2. Change `"file"` path accordingly
3. Save and restart

#### Customize JVM Memory Settings

**Example:** Increase memory for high-volume pharmacy:

1. Change `"heap_min": "512m"` to `"heap_min": "2g"`
2. Change `"heap_max": "1g"` to `"heap_max": "2g"`
3. Save and restart

**For detailed JVM customization, see:** [CUSTOMIZE-JVM-OPTIONS.md](CUSTOMIZE-JVM-OPTIONS.md)

#### Customize Application Properties

You can also customize Spring Boot application properties in `config.json`:

**File Paths:**

```json
{
  "file": {
    "report": "./reports",
    "images": "./images",
    "import": {
      "json": "./json",
      "csv": "./csv",
      "excel": "./excel"
    },
    "pharmaml": "pharmaml"
  }
}
```

**FNE Configuration (French pharmacy invoicing):**

```json
{
  "fne": {
    "url": "http://54.247.95.108/ws/external/invoices/sign",
    "api-key": "nSXimInFusKqICZaJ95QZvQT85FOZvHW",
    "point-of-sale": ""
  }
}
```

**Mail Configuration:**

```json
{
  "mail": {
    "username": "easyshopws@gmail.com",
    "email": "badoukobena@gmail.com"
  }
}
```

**Port-Com Configuration (Legacy URL):**

```json
{
  "port-com": {
    "legacy-url": "http://localhost:9090/laborex"
  }
}
```

All these properties are passed to Spring Boot as command-line arguments and override the default values in `application.yml`.

### Testing

After changing the port, test the backend:

1. Start PharmaSmart
2. Open browser and go to: `http://localhost:9080/management/health`
   (Replace 9080 with your configured port)
3. You should see health status information

---

## Remote Backend Configuration (backend-url.txt)

### What is this for?

Configure the frontend to connect to a **remote backend server** (on another computer or different port).

**Note:** A template file `backend-url.txt.template` is included in your installation with examples and instructions.

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

**Option A: Use the template (Recommended)**
1. Find `backend-url.txt.template` in your installation folder
2. **Right-click** it → **Rename** → Remove `.template` so it's just `backend-url.txt`
3. Proceed to Step 3

**Option B: Create from scratch**
1. **Right-click** next to `PharmaSmart.exe`
2. Choose **New → Text Document**
3. Name it exactly: `backend-url.txt` (delete the `.txt.txt` if Windows adds it twice)

### Step 3: Edit the Config File

1. **Right-click** `backend-url.txt`
2. Choose **Open with → Notepad**
3. Type the backend address:
   ```
   http://192.168.1.50:9080
   ```
   (Replace `192.168.1.50` with your backend computer's address from Step 1)
4. **Save** and close Notepad

### Step 4: Launch the App

Double-click `PharmaSmart.exe` - it will now connect to the backend on the other computer!

---

## Visual Guide

```
📁 Your Installation Folder
├── 📄 PharmaSmart.exe              ← Your app
├── 📄 backend-url.txt.template     ← Template (bundled with app)
├── 📄 backend-url.txt              ← Rename template to this
│      │
│      └── Contains: http://192.168.1.50:9080
└── 📄 config.json                  ← Bundled backend config (standalone only)
```

---

## Examples

### Example 1: Backend on another computer (IP: 192.168.1.100)

**Edit `backend-url.txt` to contain:**

```
http://192.168.1.100:9080
```

### Example 2: Backend on different port (8080)

**Edit `backend-url.txt` to contain:**

```
http://localhost:8080
```

### Example 3: Backend with a computer name

**Edit `backend-url.txt` to contain:**

```
http://SERVER-PC:9080
```

---

## Testing the Connection

Before launching PharmaSmart, test if you can reach the backend:

1. Open your web browser (Chrome, Firefox, etc.)
2. Type in the address bar:
   ```
   http://192.168.1.50:9080/management/health
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
