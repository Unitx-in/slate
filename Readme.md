# Slate - Lifecycle-Aware Bottom Sheet Manager

[![](https://jitpack.io/v/YOUR_USERNAME/Slate.svg)](https://jitpack.io/#YOUR_USERNAME/Slate)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A powerful, lifecycle-aware bottom sheet manager for Android that simplifies bottom sheet
implementation with built-in blur overlays, state management, and automatic cleanup.

## ✨ Features

- 🔄 **Lifecycle-Aware**: Automatic cleanup on lifecycle destruction
- 🎨 **Blur Overlay**: Built-in blur background with smooth animations
- 📱 **Back Press Handling**: Smart back navigation support
- 🎯 **State Management**: Easy state transitions with observer pattern
- 🔧 **Customizable**: Strategy pattern for custom transition behaviors
- 🏗️ **Builder Pattern**: Fluent API for easy configuration
- 🧩 **Composite Controls**: Organized button management system
- ♻️ **Memory Safe**: Automatic resource cleanup prevents memory leaks

## 📦 Installation

### Step 1: Add JitPack repository

Add it in your root `settings.gradle` or `build.gradle`:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

### Step 2: Add the dependency

```gradle
dependencies {
	        implementation ("com.github.Unitx-in:slate:v1.0.0")
	}
```

### Basic Usage

```kotlin
class MyFragment : Fragment() {
    private var slate: Slate<MySheetBinder>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slate = SlateBuilder<MySheetBinder>()
            .peekHeight(200)
            .hideable(true)
            .draggable(true)
            .onStateChange { state: Int->
                when (state) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // Handle expanded
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        // Handle hidden
                    }
                }
            }
            .build(
                currentInstance = slate,
                hostView = binding.container,
                lifecycleOwner = viewLifecycleOwner,
                onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
                bindingListener = object : Slate.BindingListener<MySheetBinder> {
                    override fun onBindSheet(hostView: View): MySheetBinder = 
                        hostView.inflateBinder<BottomSheetLayoutBinding, ViewBinder> { ViewBinder(it) }

                    override fun onBindView(binder: MySheetBinder) = binder.bind()
                }
            )
            .expand()
    }

    override fun onDestroy() {
        slate = null
    }
}
```

### Define Your ViewBinder

```kotlin
class MySheetBinder(private val binding: BottomSheetLayoutBinding) : Slate.ViewBinder(BottomSheetLayoutBinding.root) {
    fun bind(){
        binding.apply {
            
            setSaveBtn = bCtIvSave
            setCollapseBtn = bCtIvCollapse
            setAddNewBtn = bCtIvAddNew

            onStateChangedFromBinder = { state: Int->
                when (state) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // Handle expanded
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        // Handle hidden
                    }
                }
            }
            
            bCtIvSave.appendClickListener({
                onBinderAddCategoryClickListener.onSave(bCtEtName.text.toString())
                bCtEtName.text?.clear()
            })
        }
    }
}
```

## 📖 Usage Examples

### 1. Basic Configuration

```kotlin
val slate = SlateBuilder<MySheetBinder>()
    .peekHeight(300)                    // Height when collapsed
    .fitToContents(true)                // Fit to content height
    .hideable(true)                     // Can be hidden by dragging
    .skipCollapsed(false)               // Show collapsed state
    .draggable(true)                    // User can drag
    .halfExpandedRatio(0.5f)            // Half-expanded at 50%
    .build() // build(...)
    .expand()
```

### 2. State Management

```kotlin
// Expand the sheet
slate?.expand()

// Collapse to peek height
slate?.collapse()

// Hide completely
slate?.hide()

// Set specific state
slate?.setState(BottomSheetBehavior.STATE_HALF_EXPANDED)

// Check current state
if (slate?.isExpanded == true) {
    // Sheet is expanded
}
```

### 3. State Change Observers

#### Option A: Simple Lambda

```kotlin
SlateBuilder<MySheetBinder>()
    .onStateChange { state ->
        when (state) {
            BottomSheetBehavior.STATE_EXPANDED -> handleExpanded()
            BottomSheetBehavior.STATE_COLLAPSED -> handleCollapsed()
            BottomSheetBehavior.STATE_HIDDEN -> handleHidden()
        }
    }
    .build(...)
```

#### Option B: Multiple Observers

```kotlin
val analyticsObserver = object : SlateOnStateChangeObserver {
    override fun onStateChanged(state: Int) {
        logAnalyticsEvent("sheet_state", state)
    }
}

val uiObserver = object : SlateOnStateChangeObserver {
    override fun onStateChanged(state: Int) {
        updateUI(state)
    }

    override fun onSlide(offset: Float) {
        updateParallaxEffect(offset)
    }
}

SlateBuilder<MySheetBinder>()
    .addObserver(analyticsObserver)
    .addObserver(uiObserver)
    .build(...)
```

#### Option C: ViewBinder Callback

```kotlin
class MySheetBinder(private val binding: BottomSheetLayoutBinding) : Slate.ViewBinder(BottomSheetLayoutBinding.root) {
    fun bind(){
        onStateChangedFromBinder = { state ->
            // Handle state changes
        }
    }
}
```

### 4. Custom State Transitions

Create a custom strategy for unique transition effects:

```kotlin
class CustomTransitionStrategy : StateTransitionStrategy<MySheetBinder> {
    override fun onExpanded(slate: Slate<MySheetBinder>) {
        slate.arrowDown()
        slate.blurVisible()
        // Add custom behavior
        slate.binder.headerView.animate().alpha(1f).start()
        vibrate()
    }

    override fun onCollapsed(slate: Slate<MySheetBinder>) {
        slate.arrowUp()
        slate.blurHide()
        slate.binder.headerView.animate().alpha(0.5f).start()
    }

    override fun onHidden(slate: Slate<MySheetBinder>) {
        slate.blurHide()
        logEvent("sheet_dismissed")
    }

    override fun onSlide(slate: Slate<MySheetBinder>, slideOffset: Float) {
        slate.blurOffSet(slideOffset)
        // Custom parallax effect
        slate.binder.backgroundView.translationY = slideOffset * 100
    }
}

// Use the custom strategy
SlateBuilder<MySheetBinder>()
    .stateTransitionStrategy(CustomTransitionStrategy())
    .build(...)
```

### 5. External BottomSheetCallback

For direct BottomSheetBehavior integration:

```kotlin
val externalCallback = object : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
        // Your custom logic
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        // Your slide logic
    }
}

SlateBuilder<MySheetBinder>()
    .bottomSheetCallback(externalCallback)
    .build(...)
```

### 6. Singleton Pattern Support

Maintain single instance across configuration changes:

```kotlin
class MyFragment : Fragment() {
    private var slate: Slate<MySheetBinder>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        slate = SlateBuilder<MySheetBinder>()
            .peekHeight(200)
            .build(
                currentInstance = slate,  // Reuse existing instance
                hostView = binding.container,
                lifecycleOwner = viewLifecycleOwner,
                onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
                bindingListener = createBindingListener()
            )
    }
}
```

## 🎨 Customization

### Overlay Colors

```kotlin
class MySheetBinder(private val binding: BottomSheetLayoutBinding) : Slate.ViewBinder(BottomSheetLayoutBinding.root) {
    fun bind(){
        setOverlayColor = OverlayColor.Light  // or OverlayColor.Dark
    }
}
```

### Auto-Hide Buttons

Slate automatically hides the sheet when these buttons are clicked:

```kotlin
class MySheetBinder(private val binding: BottomSheetLayoutBinding) : Slate.ViewBinder(BottomSheetLayoutBinding.root) {
    fun bind(){
        binding.apply{
            setSaveBtn = saveBtn     // Auto-hides on click
            setAddNewBtn = addNewBtn  // Auto-hides on click
            setCollapseBtn = collapseBtn// Toggle expand/collapse
        }
    }
}
```

### Collapse Button Behavior

The collapse button automatically:

- Shows ↓ arrow when expanded
- Shows ↑ arrow when collapsed
- Toggles between states on click
- Hides sheet if `skipCollapsed = true`

## 🏗️ Architecture

Slate uses multiple design patterns for clean, maintainable code:

### Design Patterns Used

1. **Builder Pattern** - Fluent API for configuration
2. **Strategy Pattern** - Customizable state transitions
3. **Observer Pattern** - Multiple state change listeners
4. **Composite Pattern** - Organized button management
5. **Facade Pattern** - Simplified BottomSheetBehavior API

### Component Overview

```
┌─────────────────────────────────────────────────────────┐
│                    SlateBuilder                          │
│              (Configuration & Creation)                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│                       Slate                              │
│              (Main Coordinator Class)                    │
└──┬────────────┬──────────────┬─────────────┬───────────┘
   │            │              │             │
   ↓            ↓              ↓             ↓
┌──────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│State │  │  Slate   │  │  Slate   │  │  Slate   │
│Trans │  │Behaviour │  │Controls  │  │Observer  │
│Strategy│  │(Facade)  │  │Composite │  │Observable│
└──────┘  └──────────┘  └──────────┘  └──────────┘
```

## 📋 API Reference

### SlateBuilder Methods

| Method                              | Description                           |
|-------------------------------------|---------------------------------------|
| `peekHeight(Int)`                   | Height when collapsed (default: 0)    |
| `fitToContents(Boolean)`            | Fit to content height (default: true) |
| `hideable(Boolean)`                 | Can be hidden (default: true)         |
| `skipCollapsed(Boolean)`            | Skip collapsed state (default: false) |
| `draggable(Boolean)`                | User can drag (default: true)         |
| `halfExpandedRatio(Float)`          | Half-expanded ratio (default: 0.5f)   |
| `stateTransitionStrategy(Strategy)` | Custom transition strategy            |
| `bottomSheetCallback(Callback)`     | External BottomSheet callback         |
| `addObserver(Observer)`             | Add state change observer             |
| `onStateChange(Lambda)`             | Simple state change callback          |
| `build(...)`                        | Build and initialize Slate instance   |

### Slate Methods

| Method          | Description             |
|-----------------|-------------------------|
| `expand()`      | Expand to full height   |
| `collapse()`    | Collapse to peek height |
| `hide()`        | Hide completely         |
| `setState(Int)` | Set specific state      |
| `release()`     | Clean up resources      |
| `isExpanded`    | Check if expanded       |
| `isCollapsed`   | Check if collapsed      |
| `isHidden`      | Check if hidden         |

### ViewBinder Properties

| Property                   | Type               | Description                   |
|----------------------------|--------------------|-------------------------------|
| `setSaveBtn`               | `ImageView?`       | Button that auto-hides sheet  |
| `setCollapseBtn`           | `RadioImage?`      | Toggle expand/collapse button |
| `setAddNewBtn`             | `View?`            | Button that auto-hides sheet  |
| `setOverlayColor`          | `OverlayColor`     | Blur overlay color theme      |
| `onStateChangedFromBinder` | `((Int) -> Unit)?` | State change callback         |

## ⚠️ Important Notes

### Memory Management

```kotlin
override fun onDestroyView() {
    slate = null
    super.onDestroyView()
}
```

### Lifecycle Awareness

Slate automatically:

- Removes callbacks on lifecycle destruction
- Cleans up views and observers
- Prevents memory leaks

### Thread Safety

All state changes are posted to the main thread:

```kotlin
slate?.expand()  // Safe to call from any thread
```

## 🐛 Troubleshooting

### Sheet not visible

```kotlin
// Make sure to call expand() after build()
slate = SlateBuilder<MySheetBinder>()
    .build(...)
.expand()  // ← Don't forget this!
```

### Back press not working

```kotlin
// Ensure you pass the correct dispatcher
.build(
    onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
    // ...
)
```

### State changes not triggering

```kotlin
// Add observer before building
SlateBuilder<MySheetBinder>()
    .onStateChange { state -> /* ... */ }  // ← Before build()
    .build(...)
```

## 📄 License

```
Copyright 2025 [Navneet/Unitx]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 Support

- Create an [Issue](https://github.com/Unitx-in/Slate/issues)
- Email: developer@unitx.in

## 🌟 Show your support

Give a ⭐️ if this project helped you!

---

Made with ❤️ by [Navneet/Unitx]