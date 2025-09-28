#!/usr/bin/env bash
set -euo pipefail

echo "[init-dev] Initialisation des dépendances pour le workspace..."

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Liste de paquets nécessaires pour exécuter Chromium (Puppeteer) en headless sur Ubuntu/Debian,
# ainsi que GraphViz requis par le projet.
APT_PKGS=(
  ca-certificates
  fonts-liberation
  graphviz
  # libasound2 (Ubuntu 24.04 utilise libasound2t64)
  libatk-bridge2.0-0
  libatk1.0-0
  libcairo2
  libcups2
  libdbus-1-3
  libexpat1
  libfontconfig1
  libgdk-pixbuf2.0-0
  libglib2.0-0
  libgtk-3-0
  libnspr4
  libnss3
  libpango-1.0-0
  libpangocairo-1.0-0
  libstdc++6
  libx11-6
  libx11-xcb1
  libxcb1
  libxcb-dri3-0
  libxcomposite1
  libxcursor1
  libxdamage1
  libxext6
  libxfixes3
  libxi6
  libxrandr2
  libxrender1
  libxshmfence1
  libxss1
  libxtst6
  libgbm1
  wget
  xdg-utils
)

if command -v apt-get >/dev/null 2>&1; then
  echo "[init-dev] Installation des dépendances système via apt-get..."
  if [ "${EUID:-$(id -u)}" -ne 0 ]; then
    SUDO="sudo"
  else
    SUDO=""
  fi
  ${SUDO} apt-get update -y

  # Résolution libasound pour distributions récentes (noble t64)
  LIBASOUND_CANDIDATES=(libasound2t64 libasound2)
  LIBASOUND_SELECTED=""
  for pkg in "${LIBASOUND_CANDIDATES[@]}"; do
    if apt-cache show "$pkg" >/dev/null 2>&1; then
      LIBASOUND_SELECTED="$pkg"
      break
    fi
  done
  if [ -z "$LIBASOUND_SELECTED" ]; then
    echo "[init-dev] Avertissement: aucun paquet libasound compatible trouvé (libasound2/libasound2t64)."
  else
    APT_PKGS+=("$LIBASOUND_SELECTED")
  fi

  # --no-install-recommends pour garder l'image légère
  ${SUDO} apt-get install -y --no-install-recommends "${APT_PKGS[@]}"
  ${SUDO} apt-get clean
  ${SUDO} rm -rf /var/lib/apt/lists/*
else
  echo "[init-dev] 'apt-get' introuvable. Ignorer l'installation des paquets système."
fi

echo "[init-dev] Terminé."
