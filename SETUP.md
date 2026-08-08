# How to run this program

Written for someone who has never used GitHub or a terminal before. It takes about five
minutes, and you only have to do steps 1 and 2 once.

The program is plain Java, so it runs the same on Windows, macOS and Linux. There is nothing
to install except Java itself — no libraries, no accounts, no internet connection once you
have the files.

---

## Step 1 — Get the files

**You do not need a GitHub account, and you do not need to install git.**

1. Open the project page on GitHub in your browser.
2. Click the green **`< > Code`** button near the top right.
3. Click **Download ZIP** at the bottom of the menu that appears.
4. Find the downloaded ZIP file (usually in your Downloads folder) and unzip it:
   - **Windows** — right-click the ZIP, choose *Extract All…*, then *Extract*.
   - **macOS** — double-click the ZIP.
5. You now have a folder called something like `event-management-system-main`.
   Move it somewhere you can find again, such as your Desktop.

> **Important on Windows:** you must extract the ZIP. If you double-click the ZIP and run the
> program from inside the preview window, it will fail. Extract first.

---

## Step 2 — Install Java (the JDK)

You need the **JDK**, not just "Java". A plain Java runtime can *run* programs but cannot
*compile* them, and this project compiles itself when it starts. If you have Java already but
only the runtime, the program will tell you so and print what to install.

### Windows

Open **PowerShell** (press Start, type `powershell`, press Enter) and paste:

```
winget install EclipseAdoptium.Temurin.21.JDK
```

If that doesn't work, download the **.msi** installer from <https://adoptium.net> and run it,
accepting the default options.

### macOS

If you have Homebrew:

```
brew install --cask temurin
```

Otherwise download the **.pkg** installer for your Mac from <https://adoptium.net> and run it.
Pick the **aarch64** version for an Apple Silicon Mac (M1/M2/M3/M4), or **x64** for an older
Intel Mac. If you're unsure, click the Apple menu → About This Mac and look at "Chip".

### Linux (Ubuntu / Debian)

```
sudo apt update && sudo apt install default-jdk
```

### Check it worked

Close any terminal windows you had open, open a fresh one, and type:

```
javac -version
```

You should see something like `javac 21.0.5`. If you instead see "not recognized" or "command
not found", the install didn't finish, or you need to close and reopen the terminal — the
terminal only notices a new program when it starts up.

---

## Step 3 — Run it

### Windows

Open the project folder and **double-click `run.bat`**.

That's it. A black window opens, it says "Compiling…" then "Starting…", and the program runs.

To open the windowed version instead, hold **Shift**, right-click an empty part of the folder,
choose *Open PowerShell window here*, and type:

```
.\run.bat gui
```

### macOS and Linux

1. Open **Terminal** (on a Mac: press Cmd+Space, type `terminal`, press Enter).
2. Type `cd ` — with a space after it — but **don't press Enter yet**.
3. Drag the project folder from Finder onto the Terminal window. It fills in the path for you.
4. Now press Enter.
5. Type this and press Enter:

```
bash run.sh
```

For the windowed version:

```
bash run.sh gui
```

---

## What you should see

The command line version prints a table of three sample events and a `events>` prompt. Type
`help` to see what you can do. Type `quit` to leave.

The windowed version opens a window with the same three events in a table, and buttons along
the bottom.

Both versions share the same data. Anything you add in one shows up in the other.

---

## If something goes wrong

| What you see | What it means | What to do |
|---|---|---|
| `'javac' is not recognized` (Windows)<br>`javac: command not found` (Mac/Linux) | The JDK isn't installed, or the terminal was open before you installed it | Do Step 2, then **close the terminal and open a new one** |
| `run.sh: /bin/bash^M: bad interpreter` | The file got Windows line endings | Type `bash run.sh` instead of `./run.sh`. If it persists, tell the person who set up the repo — a `.gitattributes` file prevents this |
| `Permission denied` when running `./run.sh` | The file isn't marked as runnable | Use `bash run.sh` instead — that always works |
| The window flashes and disappears (Windows) | The program hit an error and closed too fast to read | Open PowerShell in the folder and run `.\run.bat` from there so the message stays on screen |
| `Error: Could not find or load main class` | You're running it from the wrong folder | Make sure you're inside the folder that contains `run.bat` / `run.sh`, not one level above it |
| `Unsupported class file major version` | You have two Java versions installed and they disagree | Delete the `build` folder and run again |
| The program starts but shows no events | The `data` folder didn't come across | Re-extract the ZIP; make sure the `data` folder came with it. The program still works, you'll just start with an empty list |

---

## Starting over

All the saved events live in one file: **`data/events.bin`**. Delete it and the program starts
with an empty list. Nothing else is stored anywhere on your computer.

The `build` folder is just the compiled program. It is safe to delete at any time — it gets
rebuilt automatically the next time you run.
