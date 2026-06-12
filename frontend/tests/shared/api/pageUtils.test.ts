import { describe, it, expect } from 'vitest';
import { normalizePage, toSpringPage } from '~/shared/api/pageUtils';

describe('pageUtils', () => {
  it('toSpringPage converts 1-based UI page to 0-based Spring page', () => {
    expect(toSpringPage(1)).toBe(0);
    expect(toSpringPage(2)).toBe(1);
    expect(toSpringPage(0)).toBe(0);
  });

  it('normalizePage extracts content from nested PageResponse.data', () => {
    const result = normalizePage<{ id: string }>({
      success: true,
      code: 200,
      message: 'ok',
      data: {
        content: [{ id: '1' }],
        pageNumber: 1,
        pageSize: 10,
        totalElements: 1,
        totalPages: 1,
        isLast: true,
      },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].id).toBe('1');
  });

  it('normalizePage returns empty page when shape is unknown', () => {
    const result = normalizePage({ foo: 'bar' });
    expect(result.content).toEqual([]);
  });
});
