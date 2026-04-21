# Boggrt

Boggrt is a mock API server that serves responses based on configurable endpoints and conditions. The goal is to provide
a simple and flexible way to mock API responses for testing and development purposes.

## Features

- Configure multiple mock API endpoints.
- Define specific conditions for incoming requests (method, path, body fields).
- Supports JSONPath-like syntax for request validation.
- Returns configured mock responses when conditions are met, otherwise returns 404.

For detailed configuration options, see [Configuration Specification](spec.md).