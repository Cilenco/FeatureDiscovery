## Feature-Discovery-View

In May 2016 Google has updated their Material Design specifications and added the [Feature Discovery](https://material.google.com/growth-communications/feature-discovery.html)  element - a new design pattern which is used to introduce new features and instructions to the user. This library provides an easy to use View of the implemented Feature Discovery.

## How to use
    DiscoveryView discoveryView = new DiscoveryView.Builder(context, view)
    .setPrimaryText(R.string.header)
    .setSecondaryText(R.string.description)
    .setOnClickListener(this)
    .build();

    discoveryView.show();

## Issues and Pull requests
Currently the View is not perfect because I do not know all the exact dimensions from the material design specs (or haven't found them). If you have knowlage of the unknowen dimensions or any new ideas for this library please feel free to report issues and make pull requests to this repository.

Knowen issues are:
- Time of the pulse effect to long
- Spacing between primary and secondary text in dp or sp
- Radius of the ripple in dp
