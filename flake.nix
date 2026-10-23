{
  description = "Being a Java Dev Shell";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" ];
      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in
    {
      devShells = forAllSystems (pkgs:
        let
          jdk = pkgs.jdk25;
          runtimeLibs = with pkgs; [
            libGL
            glfw
            openal
            flite
            libpulseaudio
            udev
            wayland
            libxkbcommon
            libx11
            libxcursor
            libxext
            libxrandr
            libxxf86vm
            vulkan-loader
          ];
        in
        {
          default = pkgs.mkShell {
            packages = [ jdk ];

            JAVA_HOME = "${jdk}";
          };
        });
    };
}
