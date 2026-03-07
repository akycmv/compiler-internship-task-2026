{
  description = "MiniKotlin transpiler";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in {
        devShells.default = pkgs.mkShell {
          packages = [
            pkgs.jdk17
            pkgs.gradle
            pkgs.antlr4
          ];

          JAVA_HOME = "${pkgs.jdk17}";

          shellHook = ''
            export GRADLE_USER_HOME="$PWD/.gradle-cache"
          '';
        };
      });
}

