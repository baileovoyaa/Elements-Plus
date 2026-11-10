Now you can create your own custom components.

Custom components let you implement your own logic without being limited to built-in components, and customize the name, icon, color, and pin layout.

Components cannot be nested infinitely. A custom component can only be nested inside a larger circuit; for example, a small circuit can only be nested in a medium or larger circuit.

Put your circuit diagram in the slot above, fill in the information below, then click the start button to compile it. To distinguish them, custom component names are shown in italics. Custom components can share names, so they are uniquely identified by UUID.

```epp-experiments
elements-plus:custom_component
```

Note: any scale (including infinite) can be nested in an infinite-scale circuit, so nesting depth is unlimited. You can verify very complex circuits in a creative-mode save, but be careful: deep nesting may cause lag.
