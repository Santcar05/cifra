/**
 * HeroScene — el visual del héroe de CIFRA en 3D.
 *
 * Motor propio sobre canvas 2D. No hay Three.js y no hace falta: lo que
 * produce la sensación de volumen es proyección en perspectiva real, y eso
 * son treinta líneas de matemáticas, no 600 KB de dependencia.
 *
 * Cada punto vive en {x, y, z} y se proyecta con s = FOV / (FOV + z). Ese
 * mismo factor `s` modula TRES cosas a la vez — posición, radio y opacidad —
 * y esa coincidencia es justo lo que el ojo lee como profundidad. Si solo
 * escalara el tamaño, se vería plano.
 *
 * La estructura gira lento sobre el eje Y. Es lo que convierte un dibujo en
 * un objeto: las curvas que estaban delante pasan detrás y se atenúan.
 *
 * Contrato: mount(container) devuelve su propia función de limpieza.
 */

type Vec3 = { x: number; y: number; z: number };
type Curve = { p0: Vec3; p1: Vec3; p2: Vec3; p3: Vec3 };

/** Distancia focal. Más bajo = perspectiva más agresiva. */
const FOV = 900;

/**
 * Ancho del espacio de diseño: define cuánto entra en el encuadre.
 *
 * La geometría lo DESBORDA a propósito. Las curvas arrancan fuera del
 * lienzo y salen por el otro lado, así que el canvas las recorta en los
 * bordes — que es exactamente lo que las hace leer como corrientes que
 * atraviesan, y no como un destello con principio y fin visibles flotando
 * en medio del vacío.
 */
const DESIGN_W = 1000;

/**
 * La estructura OSCILA, no da la vuelta entera.
 *
 * Una rotación completa parece la opción obvia y es un error: esta
 * composición es un abanico que converge, y vista de canto se aplana hasta
 * volverse ilegible durante media vuelta. Un vaivén de ±0.38 rad mantiene
 * la figura siempre legible y conserva todo el paralaje — que es de donde
 * sale la sensación de volumen, no de completar los 360°.
 */
const SWING_MS = 26_000;
const SWING_AMP = 0.38;

/** Desplazado arriba y a la izquierda: abajo a la derecha va la tarjeta. */
const NODE: Vec3 = { x: 10, y: -70, z: 0 };

const v = (x: number, y: number, z: number): Vec3 => ({ x, y, z });

/**
 * Las seis entradas. Vienen de lejos, por la izquierda y por arriba, a
 * distintas profundidades, y todas terminan en el nodo. Los pares de Z
 * opuestos son los que hacen que al girar se crucen de verdad.
 */
const INBOUND: Curve[] = [
  { p0: v(-700, -300, -130), p1: v(-330, -250, -68), p2: v(-120, -150, -20), p3: NODE },
  { p0: v(-720, -110, 115), p1: v(-350, -104, 66), p2: v(-135, -96, 16), p3: NODE },
  { p0: v(-710, 70, -72), p1: v(-340, 46, -35), p2: v(-130, -40, -6), p3: NODE },
  { p0: v(-670, 260, 90), p1: v(-310, 190, 46), p2: v(-120, 16, 12), p3: NODE },
  { p0: v(-260, -540, -108), p1: v(-140, -370, -54), p2: v(-46, -206, -12), p3: NODE },
  { p0: v(230, -560, 100), p1: v(178, -372, 56), p2: v(92, -200, 14), p3: NODE },
];

/** Las cuatro salidas. La primera es el trazo grueso; las otras, oliva. */
const OUTBOUND: Curve[] = [
  { p0: NODE, p1: v(230, -68, 0), p2: v(430, -76, 0), p3: v(640, -84, 0) },
  { p0: NODE, p1: v(210, 10, -32), p2: v(410, 110, -64), p3: v(610, 186, -92) },
  { p0: NODE, p1: v(224, -30, 38), p2: v(428, 4, 66), p3: v(628, 30, 88) },
  { p0: NODE, p1: v(214, -160, -16), p2: v(412, -248, -32), p3: v(596, -318, -48) },
];

const OUT_WIDTH = [2.5, 1, 1, 1];

function bezier(c: Curve, t: number): Vec3 {
  const u = 1 - t;
  const a = u * u * u;
  const b = 3 * u * u * t;
  const d = 3 * u * t * t;
  const e = t * t * t;
  return {
    x: a * c.p0.x + b * c.p1.x + d * c.p2.x + e * c.p3.x,
    y: a * c.p0.y + b * c.p1.y + d * c.p2.y + e * c.p3.y,
    z: a * c.p0.z + b * c.p1.z + d * c.p2.z + e * c.p3.z,
  };
}

/** Rotación sobre Y (el giro principal) y luego sobre X (la inclinación). */
function rotate(p: Vec3, ay: number, ax: number): Vec3 {
  const cy = Math.cos(ay);
  const sy = Math.sin(ay);
  const x1 = p.x * cy - p.z * sy;
  const z1 = p.x * sy + p.z * cy;

  const cx = Math.cos(ax);
  const sx = Math.sin(ax);
  const y2 = p.y * cx - z1 * sx;
  const z2 = p.y * sx + z1 * cx;

  return { x: x1, y: y2, z: z2 };
}

/** Lee un token de color y lo deja en [r, g, b] para poder variar el alfa. */
function readRgb(el: Element, name: string, fallback: [number, number, number]) {
  const raw = getComputedStyle(el).getPropertyValue(name).trim();
  if (!raw) return fallback;

  if (raw.startsWith('#')) {
    const h = raw.slice(1);
    const full = h.length === 3 ? h.replace(/./g, (c) => c + c) : h;
    if (full.length < 6) return fallback;
    return [
      parseInt(full.slice(0, 2), 16),
      parseInt(full.slice(2, 4), 16),
      parseInt(full.slice(4, 6), 16),
    ] as [number, number, number];
  }

  const nums = raw.match(/[\d.]+/g);
  if (nums && nums.length >= 3) {
    return [Number(nums[0]), Number(nums[1]), Number(nums[2])] as [number, number, number];
  }
  return fallback;
}

type Particle = {
  /** 0–1 recorre la entrada; 1–2 recorre la salida. El nodo está en 1. */
  t: number;
  speed: number;
  inIdx: number;
  outIdx: number;
  /** Para no disparar el pulso del nodo dos veces en el mismo paso. */
  crossed: boolean;
};

export function mount(container: HTMLElement): () => void {
  const canvas = document.createElement('canvas');
  canvas.className = 'hero-visual__canvas';
  // Decorativo: la información ya la da el texto del héroe.
  canvas.setAttribute('aria-hidden', 'true');

  const ctx = canvas.getContext('2d');
  if (!ctx) return () => {};

  container.appendChild(canvas);

  const ink = readRgb(container, '--on-ink-primary', [247, 246, 243]);
  const olive = readRgb(container, '--olive-400', [140, 162, 84]);

  let w = 0;
  let h = 0;
  let scale = 1;
  let raf = 0;
  let running = false;
  let firstFrame = true;

  // Inclinación con el puntero, amortiguada.
  let targetTiltX = 0;
  let targetTiltY = 0;
  let tiltX = 0;
  let tiltY = 0;

  let nodePulse = 0;

  const particles: Particle[] = [];
  const PER_CURVE = 13;
  for (let i = 0; i < INBOUND.length; i++) {
    for (let k = 0; k < PER_CURVE; k++) {
      particles.push({
        t: (k / PER_CURVE) * 2 + Math.random() * 0.06,
        speed: 0.055 + Math.random() * 0.03,
        inIdx: i,
        outIdx: Math.floor(Math.random() * OUTBOUND.length),
        crossed: false,
      });
    }
  }

  function resize() {
    const rect = container.getBoundingClientRect();
    if (rect.width === 0) return;

    // Tope a 2: por encima no se gana nitidez visible y sí se paga relleno.
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    w = rect.width;
    h = rect.height;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    canvas.style.width = `${w}px`;
    canvas.style.height = `${h}px`;
    ctx!.setTransform(dpr, 0, 0, dpr, 0, 0);
    scale = w / DESIGN_W;
  }

  /** Proyecta a coordenadas de pantalla. `s` es el factor de profundidad. */
  function project(p: Vec3) {
    const s = FOV / (FOV + p.z);
    return {
      x: w / 2 + p.x * s * scale,
      y: h / 2 + p.y * s * scale,
      s,
    };
  }

  function drawCurve(c: Curve, ay: number, ax: number, rgb: number[], alpha: number, width: number) {
    const STEPS = 26;
    let zSum = 0;

    ctx!.beginPath();
    for (let i = 0; i <= STEPS; i++) {
      const p = rotate(bezier(c, i / STEPS), ay, ax);
      zSum += p.z;
      const q = project(p);
      if (i === 0) ctx!.moveTo(q.x, q.y);
      else ctx!.lineTo(q.x, q.y);
    }

    // La curva entera se atenúa según su profundidad media. Al girar, las
    // que pasan atrás se apagan — ese contraste es la mitad del efecto.
    const zAvg = zSum / (STEPS + 1);
    const depth = Math.max(0.35, Math.min(1.25, 1 - zAvg / 620));

    ctx!.strokeStyle = `rgba(${rgb[0]}, ${rgb[1]}, ${rgb[2]}, ${alpha * depth})`;
    ctx!.lineWidth = Math.max(0.6, width * scale * depth);
    ctx!.lineCap = 'round';
    ctx!.stroke();
  }

  function drawNode(ay: number, ax: number) {
    const R = 26;
    const corners: Vec3[] = [
      { x: NODE.x, y: NODE.y - R, z: NODE.z },
      { x: NODE.x + R * 0.62, y: NODE.y, z: NODE.z },
      { x: NODE.x, y: NODE.y + R, z: NODE.z },
      { x: NODE.x - R * 0.62, y: NODE.y, z: NODE.z },
    ];

    ctx!.beginPath();
    corners.forEach((c, i) => {
      const q = project(rotate(c, ay, ax));
      if (i === 0) ctx!.moveTo(q.x, q.y);
      else ctx!.lineTo(q.x, q.y);
    });
    ctx!.closePath();

    const a = 0.5 + nodePulse * 0.45;
    ctx!.strokeStyle = `rgba(${ink[0]}, ${ink[1]}, ${ink[2]}, ${a})`;
    ctx!.lineWidth = Math.max(0.8, 1.15 * scale);
    ctx!.stroke();
  }

  let last = performance.now();

  function frame(now: number) {
    const dt = Math.min((now - last) / 1000, 0.05);
    last = now;

    const ay = Math.sin((now / SWING_MS) * Math.PI * 2) * SWING_AMP;

    // Amortiguación de la inclinación: sin esto el puntero la hace saltar.
    tiltX += (targetTiltX - tiltX) * 0.06;
    tiltY += (targetTiltY - tiltY) * 0.06;
    const ayTotal = ay + tiltY;

    nodePulse = Math.max(0, nodePulse - dt * 2.2);

    ctx!.clearRect(0, 0, w, h);

    // 1 · Las curvas, al fondo.
    for (const c of INBOUND) {
      drawCurve(c, ayTotal, tiltX, ink, 0.22, 1.25);
    }
    for (let i = 0; i < OUTBOUND.length; i++) {
      const isThick = i === 0;
      drawCurve(
        OUTBOUND[i],
        ayTotal,
        tiltX,
        isThick ? ink : olive,
        isThick ? 0.85 : 0.7,
        OUT_WIDTH[i],
      );
    }

    // 2 · El nodo.
    drawNode(ayTotal, tiltX);

    // 3 · Las partículas, ordenadas por profundidad para que las cercanas
    //     tapen a las lejanas (algoritmo del pintor).
    type Item = { x: number; y: number; r: number; a: number; rgb: number[] };
    const items: Item[] = [];

    for (const p of particles) {
      p.t += p.speed * dt * 2;

      if (p.t >= 2) {
        p.t -= 2;
        p.outIdx = Math.floor(Math.random() * OUTBOUND.length);
        p.crossed = false;
      }

      const inbound = p.t < 1;
      if (!inbound && !p.crossed) {
        p.crossed = true;
        nodePulse = 1;
      }
      if (inbound) p.crossed = false;

      const curve = inbound ? INBOUND[p.inIdx] : OUTBOUND[p.outIdx];
      const local = inbound ? p.t : p.t - 1;
      const pos = rotate(bezier(curve, local), ayTotal, tiltX);
      const q = project(pos);

      // El mismo factor de profundidad gobierna radio y opacidad.
      const depth = Math.max(0.3, Math.min(1.5, q.s));
      const rgb = inbound ? ink : p.outIdx === 0 ? ink : olive;

      // Se desvanece al nacer y al morir, para que no aparezca de golpe.
      const edge = Math.min(1, Math.min(p.t, 2 - p.t) * 4);

      items.push({
        x: q.x,
        y: q.y,
        r: Math.max(0.9, 3.4 * depth * scale * 1.7),
        a: Math.min(1, 0.95 * depth * edge),
        rgb,
      });
    }

    items.sort((a, b) => a.r - b.r);
    for (const it of items) {
      ctx!.beginPath();
      ctx!.arc(it.x, it.y, it.r, 0, Math.PI * 2);
      ctx!.fillStyle = `rgba(${it.rgb[0]}, ${it.rgb[1]}, ${it.rgb[2]}, ${it.a})`;
      ctx!.fill();
    }

    if (firstFrame) {
      firstFrame = false;
      // Solo ahora se revela: el SVG ha hecho de póster hasta este momento.
      container.classList.add('is-canvas-ready');
    }

    if (running) raf = requestAnimationFrame(frame);
  }

  function start() {
    if (running) return;
    running = true;
    last = performance.now();
    raf = requestAnimationFrame(frame);
  }

  function stop() {
    running = false;
    cancelAnimationFrame(raf);
  }

  // --- Puntero: solo donde existe de verdad -------------------------------
  const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');

  function onPointerMove(e: PointerEvent) {
    const rect = container.getBoundingClientRect();
    const nx = (e.clientX - rect.left) / rect.width - 0.5;
    const ny = (e.clientY - rect.top) / rect.height - 0.5;
    targetTiltY = nx * 0.42;
    targetTiltX = ny * -0.28;
  }

  function onPointerLeave() {
    targetTiltY = 0;
    targetTiltX = 0;
  }

  if (finePointer.matches) {
    container.addEventListener('pointermove', onPointerMove);
    container.addEventListener('pointerleave', onPointerLeave);
  }

  // --- Solo corre cuando se ve --------------------------------------------
  const io = new IntersectionObserver(
    ([entry]) => (entry.isIntersecting ? start() : stop()),
    { threshold: 0.01 },
  );
  io.observe(container);

  const ro = new ResizeObserver(() => resize());
  ro.observe(container);

  resize();

  return () => {
    stop();
    io.disconnect();
    ro.disconnect();
    container.removeEventListener('pointermove', onPointerMove);
    container.removeEventListener('pointerleave', onPointerLeave);
    canvas.remove();
    container.classList.remove('is-canvas-ready');
  };
}
