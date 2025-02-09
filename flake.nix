{
  description = "A Nix-flake-based development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    crPkgs.url = "github:chai-yuan/crpkgs";
  };

  outputs =
    {
      self,
      nixpkgs,
      crPkgs,
    }:
    let
      supportedSystems = [
        "x86_64-linux"
        "aarch64-darwin"
        "x86_64-darwin"
      ]; # 支持的系统
      forAllSystems = f: nixpkgs.lib.genAttrs supportedSystems (system: f system);
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = import nixpkgs { inherit system; };
          crpkgs = crPkgs.packages.${system} or { };
        in
        {
          default = pkgs.mkShell {
            packages = [
              pkgs.gcc
              pkgs.gnumake
              pkgs.jdk11
              pkgs.mill
              pkgs.metals
            ];

            shellHook = ''
              echo "Welcome to the Nix environment for ${system}"
            '';
          };
        }
      );
    };
}
