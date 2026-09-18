/**
 * Los rieles por los que entra el dinero.
 *
 * Dos orígenes distintos, y por eso dos formas de declararlos:
 *
 *   `image`  Archivo aportado por el equipo (PNG/WebP), en src/assets/logos.
 *            Son mapas de bits, así que NO heredan currentColor: se
 *            normalizan por filtro CSS en LogoWall.
 *   `svg`    Trazado vectorial de simple-icons, que sí pinta con
 *            currentColor.
 *
 * Los archivos llegan en color y en blanco y negro mezclados, y con fondos
 * y márgenes distintos. El muro los iguala por filtro y por caja óptica —
 * limitando alto Y ancho— en vez de forzarlos todos a la misma altura, que
 * es lo que haría que un logotipo ancho aplastara a uno compacto.
 *
 * NOTA LEGAL: los cinco SVG vienen de simple-icons (archivo CC0). Eso cubre
 * el fichero, no la marca. Los nueve restantes los aportó el equipo.
 */

import bancolombia from '@/assets/logos/bancolombia.png';
import bold from '@/assets/logos/bold.png';
import daviplata from '@/assets/logos/daviplata.webp';
import davivienda from '@/assets/logos/davivienda.png';
import dian from '@/assets/logos/DIAN.png';
import nequi from '@/assets/logos/nequi.png';
import payu from '@/assets/logos/payU.webp';
import pse from '@/assets/logos/PSE.png';
import wompi from '@/assets/logos/wompi.png';

import type { ImageMetadata } from 'astro';

export interface Rail {
  name: string;
  /** Logotipo como archivo de imagen. */
  image?: ImageMetadata;
  /** Logotipo como trazado vectorial, viewBox 0 0 24 24. */
  svg?: string;
  /** Ajuste óptico individual: algunos logotipos piden más o menos caja. */
  scale?: number;
}

/**
 * Los valores de `scale` están ajustados A OJO contra la pantalla, uno por
 * uno. No salen de una fórmula y no deberían: un logotipo ancho y uno
 * compacto con la misma altura pesan distinto para el ojo. Un logotipo que
 * es solo una letra pide menos caja; un logotipo horizontal con texto pide
 * más. Si se cambia un archivo, hay que volver a mirar este número.
 */
export const rails: Rail[] = [
  { name: 'Nequi', image: nequi, scale: 0.68 },
  { name: 'Bancolombia', image: bancolombia, scale: 0.95 },
  { name: 'Davivienda', image: davivienda, scale: 0.78 },
  { name: 'Daviplata', image: daviplata, scale: 1.45 },
  { name: 'Wompi', image: wompi, scale: 1.05 },
  { name: 'PayU', image: payu, scale: 1.05 },
  { name: 'Bold', image: bold, scale: 1.2 },
  { name: 'PSE', image: pse, scale: 0.82 },
  { name: 'DIAN', image: dian, scale: 1.5 },

  // Trazados descargados de simple-icons y verificados, no escritos de
  // memoria. Para actualizarlos:
  //   https://cdn.jsdelivr.net/npm/simple-icons@latest/icons/<slug>.svg
  {
    name: 'Stripe',
    scale: 1.1,
    svg: '<path d="M13.976 9.15c-2.172-.806-3.356-1.426-3.356-2.409 0-.831.683-1.305 1.901-1.305 2.227 0 4.515.858 6.09 1.631l.89-5.494C18.252.975 15.697 0 12.165 0 9.667 0 7.589.654 6.104 1.872 4.56 3.147 3.757 4.992 3.757 7.218c0 4.039 2.467 5.76 6.476 7.219 2.585.92 3.445 1.574 3.445 2.583 0 .98-.84 1.545-2.354 1.545-1.875 0-4.965-.921-6.99-2.109l-.9 5.555C5.175 22.99 8.385 24 11.714 24c2.641 0 4.843-.624 6.328-1.813 1.664-1.305 2.525-3.236 2.525-5.732 0-4.128-2.524-5.851-6.594-7.305h.003z"/>',
  },
  {
    name: 'Wise',
    svg: '<path d="M6.488 7.469 0 15.05h11.585l1.301-3.576H7.922l3.033-3.507.01-.092L8.993 4.48h8.873l-6.878 18.925h4.706L24 .595H2.543l3.945 6.874Z"/>',
  },
  {
    name: 'Payoneer',
    scale: 1.25,
    svg: '<path d="M1.474 3.31c.234 1.802 1.035 5.642 1.398 7.263.095.459.201.853.298 1.013.501.865.907-.287.907-.287C5.644 6.616 3.17 3.597 2.38 2.787c-.139-.15-.384-.332-.608-.396-.32-.095-.374.086-.374.236.01.148.065.565.075.682zm21.835-1.463c.31.224 1.386 1.355 0 1.526-1.984.234-5.76.373-12.022 5.61C8.92 10.968 3.607 16.311.76 22.957a.181.181 0 01-.216.106c-.255-.074-.714-.352-.48-1.418.32-1.44 3.201-8.938 10.817-15.552 2.485-2.155 8.416-7.232 12.426-4.245z"/>',
  },
  {
    name: 'Nubank',
    scale: 1.3,
    svg: '<path d="M7.2795 5.4336c-1.1815 0-2.1846.4628-2.9432 1.252h-.002c-.0541-.0022-.1074-.002-.162-.002-1.5436 0-2.9925.8835-3.699 2.2559-.3088.5996-.4234 1.2442-.459 1.9003-.0321.589 0 1.1863 0 1.7696v5.6523H3.184s.0022-2.784 0-5.1777c-.0014-1.6112-.0118-3.0471 0-3.3418.056-1.3937.4372-2.3053 1.1484-3.0508 2.3585.0018 3.8852 1.6091 3.9705 4.168.0196.5874.0254 3.7304.0254 3.7304v3.672h3.1678v-4.965c0-1.5007.0127-2.8006-.0918-3.6952-.292-2.5-1.821-4.168-4.1248-4.168zm8.3903.3008l-3.166.0039v4.9648c0 1.5009-.0127 2.8007.0919 3.6953.2921 2.5001 1.821 4.168 4.1248 4.168 1.1815 0 2.1846-.4628 2.9432-1.252.0003-.0003.0016.0004.002 0 .0542.0023.1093.002.164.002 1.5435 0 2.9905-.8835 3.6971-2.2558.3088-.5997.4233-1.2442.459-1.9004.032-.5889 0-1.1862 0-1.7695V5.7383H20.816s-.0022 2.784 0 5.1777c.0015 1.6113.0119 3.047 0 3.3418-.056 1.3935-.4372 2.3053-1.1483 3.0508-2.3586-.0018-3.8853-1.6091-3.9706-4.168-.0196-.5874-.0273-2.0437-.0273-3.7324Z"/>',
  },
];
