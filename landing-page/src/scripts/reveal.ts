/**
 * Revelado por viewport y contadores de cifras.
 *
 * Dos comportamientos, un solo observador cada uno, para toda la página.
 *
 * NOTA DELIBERADA: el revelado NO escalona a los hijos. El efecto de cascada
 * con delays incrementales es la marca más reconocible de una landing
 * generada automáticamente. La sección entra entera, de una vez.
 */

const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/* ---------------------------------------------------------------------------
   1 · Revelado
   ------------------------------------------------------------------------- */

const revealables = document.querySelectorAll<HTMLElement>('.reveal');

if (revealables.length) {
  if (reduced) {
    revealables.forEach((el) => el.classList.add('is-in'));
  } else {
    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          entry.target.classList.add('is-in');
          // once: true. Reaparecer al volver a subir es ruido, no diseño.
          io.unobserve(entry.target);
        }
      },
      { rootMargin: '0px 0px -12% 0px', threshold: 0.05 },
    );

    revealables.forEach((el) => io.observe(el));
  }
}

/* ---------------------------------------------------------------------------
   2 · Contadores
   ------------------------------------------------------------------------- */

const formatter = new Intl.NumberFormat('es-CO');

function easeOut(t: number): number {
  return 1 - Math.pow(1 - t, 3);
}

function runCounter(el: HTMLElement) {
  const target = Number(el.dataset.countTo ?? '0');
  const prefix = el.dataset.countPrefix ?? '';
  const duration = Number(el.dataset.countDuration ?? '500');

  if (!Number.isFinite(target)) return;

  const start = performance.now();

  function step(now: number) {
    const t = Math.min((now - start) / duration, 1);
    const value = Math.round(target * easeOut(t));
    el.textContent = prefix + formatter.format(value);
    if (t < 1) requestAnimationFrame(step);
  }

  requestAnimationFrame(step);
}

const counters = document.querySelectorAll<HTMLElement>('[data-count-to]');

if (counters.length) {
  // El valor final se escribe siempre primero. Si el JS falla o nunca llega,
  // la cifra correcta ya está en pantalla — nunca un cero.
  const paint = (el: HTMLElement) => {
    const target = Number(el.dataset.countTo ?? '0');
    el.textContent = (el.dataset.countPrefix ?? '') + formatter.format(target);
  };

  if (reduced) {
    counters.forEach(paint);
  } else {
    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          runCounter(entry.target as HTMLElement);
          io.unobserve(entry.target);
        }
      },
      { threshold: 0.4 },
    );

    counters.forEach((el) => {
      paint(el);
      io.observe(el);
    });
  }
}
