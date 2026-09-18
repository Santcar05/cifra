// @ts-check
import { defineConfig } from 'astro/config';

// https://astro.build/config
export default defineConfig({
  // URL final del sitio. Necesaria para generar el sitemap
  site: 'https://cifra.co',
  // 'static' genera HTML en el build. Es lo que queremos:
  // sin servidor, desplegable en una CDN.
  output: 'static',
  // Astro por defecto genera /precios/index.html, que sirve
  // la URL /precios/ con barra final. 'never' genera /precios.html
  // y sirve /precios sin barra. Elegimos never por coherencia
  // con los enlaces que escribimos a mano.
  trailingSlash: 'never',

  build: {
    // 'file' -> /precios.html en vez de /precios/index.html
    // Coherente con trailingSlash: 'never' y más fácil de manejar en un CDN.
    format: 'file',
  },

  // Comprimir el HTML de salida. Gratis, sin contrapartidas
  compressHTML: true,

  vite: {
    css: {
      // Mensaje de error más legibles
      devSourcemap: true,
    },
  },
});
