// Универсальный composable для debounce любого значения
import { ref, watch } from 'vue';

/**
 * Debounce reactive value
 * @param {Ref} source - исходное реактивное значение
 * @param {number} delay - задержка в мс
 * @returns {Ref} debouncedValue
 */
export function useDebounce(source, delay = 500) {
  const debounced = ref(source.value);
  let timeout;

  watch(source, (val) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => {
      debounced.value = val;
    }, delay);
  });

  return debounced;
}
