import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  reporter: 'line',
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'android-compact',
      metadata: { mobile: true, platform: 'android' },
      use: { ...devices['Galaxy S9+'], browserName: 'chromium' },
    },
    {
      name: 'android-modern',
      metadata: { mobile: true, platform: 'android' },
      use: { ...devices['Pixel 7'], browserName: 'chromium' },
    },
    {
      name: 'ios-compact',
      metadata: { mobile: true, platform: 'ios' },
      use: { ...devices['iPhone SE'], browserName: 'webkit' },
    },
    {
      name: 'ios-modern',
      metadata: { mobile: true, platform: 'ios' },
      use: { ...devices['iPhone 13'], browserName: 'webkit' },
    },
  ],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 4173',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: !process.env.CI,
  },
})
