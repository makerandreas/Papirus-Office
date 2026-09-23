# Papirus Office: Inky Test Checklist
*Chapter 1: Introducing Writer*

> Based on LibreOffice Writer Guide - Chapter 1: Introducing Writer

## Document Lifecycle
Perform the following test to ensure eligibility.
- New → Save → Close → Open: The content must be identical
- New → Save → Reload: No changes in document
- Edit → Reload → Yes: All changes must be discarded
- Edit → Reload → No: Stays in **Inky Editor Screen: Editor Mode**
- Close without making any changes: Must be directed to **Start Screen: Recents**
- Close after making any changes: Popup dialog **Save before Exit** must be shown.
- Save → Close → Open the file again: Data must be retained.

## Editing Engine
Perform the following test to ensure eligibility.

### Stage 1
- Type `Hello`
- Press `Backspace`
- Tap `Undo`
- Tap `Redo`

### Stage 2
- Type `Hello`
- Press `Enter` to make a new line
- Type `World`
- Tap `Undo` twice
- Tap `Redo` Twice

### What to Expect
- Caret position is back to correct
- Paragraph is not broken
- Layout Engine does not rebuild the entire document

## Multiple Undo
Perform the following test to ensure eligibility.

- Type `abcde` *(or any random 5 characters)*
- Tap `Undo`. The editor should not display anything in the document viewfinder. 
- Tap `Redo`. The editor should display `abcde` again.

## Caret
Perform the following test to ensure eligibility.

If the hardware keyboard is present and connected, try pressing:
- `Home`
- `End`
- `Ctrl`+`↑`
- `Ctrl`+`↓`
- `↑`
- `↓`

**What to expect?**
- Caret doesn't jump
- Selection remains correct

## Selection
Perform the following test to ensure eligibility.

### Stage 1
- Type `Hello world`
- Select `Hello`
- Cut the text
- Tap `Undo`
- Tap `Redo`

### Stage 2
- Select all text (Tap and hold to open **FCT: Compact Mode** then tap `Select all`)
- Delete the text using keyboard
- Tap `Undo` on the app bar
- Delete the text again using **FCT: Compact Mode** → `Delete`
- Tap `Undo` on the app bar again
- Delete the text using keyboard
- Tap `Undo` on the **Standard Bottom Sheet: Ribbon**
- Delete the text again using **FCT: Compact Mode** → `Delete`
- Tap `Undo` on the **Standard Bottom Sheet: Ribbon** again

## Go To
Perform the following test to ensure eligibility.

- Open any ODT/DOCX document with at least 20 pages and above.
- Tap on the `Page [x] of [y]` on the bottom left on **Inky Editor Screen** at Viewer or Editor mode. The **Go to** popup dialog opens.
- Try to jump to half of document or the end of document. The editor must jumps to the target page.
- Also try to jump to pages that not covered (e.g. jump to page 21 on the 20-pages document). The editor must display a toast that the page isn't exist.

## Navigator
Perform the following test to ensure eligibility.

- Open a document that contain headings, tables, images, or etc.
- Make sure all of the items shown, the order is correct, and tapping on one of these object can jump to its exact location.

## Reminder
Perform the following test to ensure eligibility.

- Create 5 random reminders (do not have to be in order).
- Create a sixth reminder. Make sure the first reminders is automatically deleted after the creation of this sixth reminder.
- Tap `Previous` and `Next` (on the **Standard Bottom Sheet: Navigator** while the `Reminder` filter is selected) to ensure moving between each reminders correctly.

## Zoom
Perform the following test to ensure eligibility.

Try to zoom:
- 50%
- 100% (fit on page)
- 150%
- 200%
- 300%

And, at each zoom level, test:
- Scroll
- Edit
- Select text
- Trigger FCT
- Undo
- Reload

## Save Compatibility
Perform the following test to ensure eligibility.

Create and save an ODT/DOCX file, and check:
- Contents
- Headings
- Paragraphs
- Formatting (Bold, Italic, Underline, Strikethrough)
- Alignment

## Session Restore
Perform the following test to ensure eligibility.

Open any document that contains 20 pages or above, and:
- Go to page 15
- Zoom at 170%
- Scroll to the middle of the page 
- Place the cursor at the random position

Force stop the app, then open it again. Check and make sure the page position, scroll position, zoom level, and caret position still retained.

## Stress test
Perform the following test to ensure eligibility.

- Open any document that contains more than 100 pages
- Test scroll, edit, undo/redo, reload, close, save.
- Make sure the app not crashing, freeze, or hitting `OutOfMemory` error.

## Aftermath
After all the above tests are completed, the development based on Chapter 1: Introducing Writer is declared complete and can be continued to Chapter 2: Working with Text (Basics).