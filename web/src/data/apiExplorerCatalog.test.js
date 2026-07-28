import { describe, expect, it } from 'vitest'
import { allEndpoints, endpointGroups, findEndpointByKey } from './apiExplorerCatalog'

describe('apiExplorerCatalog', () => {
  it('covers every backend controller mapping', () => {
    expect(allEndpoints).toHaveLength(97)
  })

  it('contains unique testable endpoint definitions', () => {
    const keys = new Set()
    const routes = new Set()

    for (const endpoint of allEndpoints) {
      expect(endpoint.key).toBeTruthy()
      expect(endpoint.title).toBeTruthy()
      expect(endpoint.description).toBeTruthy()
      expect(endpoint.access).toBeTruthy()
      expect(['GET', 'POST', 'PUT', 'PATCH', 'DELETE']).toContain(endpoint.method)
      expect(endpoint.path).toMatch(/^\/api\//)
      expect(typeof endpoint.auth).toBe('boolean')

      const routeSignature = `${endpoint.method} ${endpoint.path}`
      expect(keys.has(endpoint.key), `duplicate key: ${endpoint.key}`).toBe(false)
      expect(routes.has(routeSignature), `duplicate route: ${routeSignature}`).toBe(false)
      keys.add(endpoint.key)
      routes.add(routeSignature)

      for (const match of endpoint.path.matchAll(/\{([^}]+)\}/g)) {
        expect(
          endpoint.pathParams?.some((param) => param.name === match[1]),
          `${routeSignature} has no input for {${match[1]}}`,
        ).toBe(true)
      }
    }
  })

  it('keeps groups and key lookup consistent', () => {
    expect(endpointGroups.length).toBeGreaterThan(0)
    for (const endpoint of allEndpoints) {
      expect(findEndpointByKey(endpoint.key)).toMatchObject({
        key: endpoint.key,
        groupKey: endpoint.groupKey,
      })
    }
  })
})
