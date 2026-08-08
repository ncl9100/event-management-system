# How to run this program

For anyone who has not used GitHub or a terminal before. Takes about 5 minutes, and you only
do steps 1 and 2 once.

It is plain Java so it works the same on Windows, Mac and Linux.

## Step 1: Get the files

You do not need a GitHub account and you do not need to install git.

1. On the GitHub page, click the green **Code** button.
2. Click **Download ZIP**.
3. Unzip it. On Windows right click the ZIP and pick "Extract All". On Mac just double click
   it.
4. Put the folder somewhere you can find it, like your Desktop.

On Windows, make sure you actually extract the ZIP. If you run it from inside the preview
window it will not work.

## Step 2: Install Java

You need the **JDK**, not just "Java". Regular Java can run programs but cannot compile them,
and this project compiles itself when it starts.

**Windows.** Open PowerShell (press Start, type powershell, hit Enter) and paste:

```
winget install EclipseAdoptium.Temurin.21.JDK
```

If that does not work, get the .msi installer from https://adoptium.net and run it.

**Mac.** If you have Homebrew:

```
brew install --cask temurin
```

Otherwise download the .pkg from https://adoptium.net. Pick aarch64 for an M1/M2/M3/M4 Mac or
x64 for an older Intel one. Apple menu > About This Mac tells you which you have.

**Linux (Ubuntu):**

```
sudo apt install default-jdk
```

To check it worked, close your terminal, open a new one, and type `javac -version`. You should
see something like `javac 21.0.5`. If it says "not recognized" or "command not found", either
the install did not finish or you need to open a fresh terminal.

## Step 3: Run it

**Windows.** Open the folder and double click `run.bat`. A black window opens, says
"Compiling..." then "Starting...", and the program runs.

For the GUI version, hold Shift, right click an empty spot in the folder, pick "Open
PowerShell window here", and type `.\run.bat gui`.

**Mac or Linux.**

1. Open Terminal (on Mac press Cmd+Space and type terminal).
2. Type `cd ` with a space after it, but do not press Enter yet.
3. Drag the project folder from Finder onto the Terminal window. It fills in the path.
4. Press Enter.
5. Type `bash run.sh` and press Enter.

For the GUI version use `bash run.sh gui`.

## What you should see

The command line version prints a table of 3 sample events and an `events>` prompt. Type
`help` to see the commands, `quit` to leave.

The GUI version opens a window with the same 3 events in a table and buttons at the bottom.

Both versions share the same data, so anything you add in one shows up in the other.

## If something goes wrong

| Problem | Fix |
|---|---|
| `'javac' is not recognized` or `command not found` | Do step 2, then close the terminal and open a new one |
| `bad interpreter` error on Mac | Type `bash run.sh` instead of `./run.sh` |
| `Permission denied` running `./run.sh` | Same fix, use `bash run.sh` |
| Window flashes and closes on Windows | Open PowerShell in the folder and run `.\run.bat` there so you can read the error |
| `Could not find or load main class` | You are in the wrong folder. Make sure you are in the one with `run.bat` in it |
| `Unsupported class file major version` | Delete the `build` folder and try again |
| No events show up | Make sure the `data` folder came out of the ZIP. The program still works, you just start empty |

## Starting over

All the saved events are in `data/events.bin`. Delete it and you start with an empty list.
Nothing else gets stored on your computer.

The `build` folder is just the compiled code. You can delete it whenever, it gets rebuilt next
time you run.
