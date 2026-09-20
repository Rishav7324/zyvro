# F-Droid Submission Guide 🤖

This guide explains the process of publishing **Zyvro** to the **F-Droid** ecosystem.

---

## 🌟 Two Primary Publishing Routes

| Method | Review Time | APK Source | Maintenance |
| :--- | :--- | :--- | :--- |
| **1. IzzyOnDroid F-Droid Repo** *(Recommended first step)* | 1–3 Days | Automatically pulls from GitHub Releases | Zero — updates automatically whenever you publish a GitHub release |
| **2. Official F-Droid Main Repo** | 2–6 Weeks | Built from source by F-Droid servers | Requires MR approval for new builds |

---

## 🚀 Route 1: Publishing on IzzyOnDroid (Fastest & Easiest)

**IzzyOnDroid** is the largest and most popular third-party F-Droid repository. Many famous open-source Android projects (like NewPipe, Seal, Spotube, ViMusic) publish on IzzyOnDroid before or alongside official F-Droid.

### Advantages:
* Uses the exact signed APKs from your GitHub Releases.
* Automatically updates when you tag a new release.
* Easily installable in the F-Droid app by adding the IzzyOnDroid repo URL.

### Steps to Submit to IzzyOnDroid:
1. Go to the [IzzyOnDroid Repository Requests Tracker](https://gitlab.com/IzzyOnDroid/repo/-/issues).
2. Click **New Issue**.
3. Select the **"Inclusion Request"** template.
4. Fill in:
   - **Application Name**: `Zyvro`
   - **Package ID**: `com.zyvro.app`
   - **Git Repository**: `https://github.com/Rishav7324/zyvro`
   - **License**: `GNU GPLv3`
   - **Releases URL**: `https://github.com/Rishav7324/zyvro/releases`
5. Submit the issue! The maintainer usually reviews and adds it within 24–48 hours.

---

## 🏛️ Route 2: Official F-Droid Main Repository

The official F-Droid repository builds all APKs directly from source in a sandboxed Debian environment.

### Prerequisites Checklist:
- [x] Open source license (**GPL-3.0-or-later**).
- [x] Source code hosted on a public git repository (**GitHub**).
- [x] Standard git version tags matching releases (`v3.0.1`).
- [x] Fastlane metadata configured in `fastlane/metadata/android/en-US/`.
- [x] Recipe created: `fastlane/com.zyvro.app.yml`.

### Step-by-Step Submission:
1. **Fork `fdroiddata` on GitLab**:
   - Go to [https://gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
   - Click **Fork** to create your copy.
2. **Add Metadata Recipe**:
   - Create a new file in your fork at `metadata/com.zyvro.app.yml`.
   - Copy the contents from [`fastlane/com.zyvro.app.yml`](fastlane/com.zyvro.app.yml).
3. **Commit and Push**:
   - Commit with message: `New app: com.zyvro.app`
4. **Open a Merge Request (MR)**:
   - Open an MR against `fdroid/fdroiddata` `master` branch.
   - The F-Droid automated CI bot (`checkupdates` and `fdroid build`) will run a test build.
   - Once F-Droid contributors review and merge it, Zyvro will be indexed in the official F-Droid catalog!
