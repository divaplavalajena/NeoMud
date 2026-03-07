# WorldSmith Agent Memory

## Zone Editor Canvas Coordinates
- The zone map is rendered on an HTML canvas element (not DOM nodes)
- Canvas internal size is 1.5x CSS size (matches devicePixelRatio=1.5)
- Room positions in the React fiber tree: canvas fiber -> memoizedProps -> rooms (array with id, name, x, y)
- Viewport offset in fiber state: {x, y} (e.g., {x:350, y:900}) and viewport size {w, h}
- Hit detection is grid-cell based, not room-rectangle based -- cells are larger than visual room boxes
- To find screen position of a room: need to know cell size and origin offset in canvas coords (not trivially derivable from visual inspection due to DPR scaling)
- Confirmed: screen (693, 557) maps to grid(0,1) when Millhaven is selected and no room panel is open
- Grid cell spacing is approximately 92px horizontal, 87px vertical in screen coordinates

## Testing Alt+Drag in Zone Editor
- Use Playwright native keyboard/mouse: page.keyboard.down('Alt') then page.mouse.move/down/move/up
- JavaScript-dispatched MouseEvents with altKey:true also work for triggering the drag
- Dropping (mouse up) can cause Playwright MCP connection to close -- but the operation still completes server-side
- Dialog auto-dismiss handler: page.on('dialog', async d => d.dismiss()) -- set before any canvas clicks
- Clicking empty grid cells triggers a confirm("Create new room at grid (x,y)?") dialog which can cascade

## Common Pitfalls
- The room properties panel opening shifts the canvas position -- room screen coordinates change when panel opens
- Regular drag (no Alt) pans the map, which changes all room positions
- Clicking on rooms from other zones changes the selected zone context
- Multiple queued confirm dialogs from rapid-clicking empty cells can overwhelm dialog handling and crash the connection
