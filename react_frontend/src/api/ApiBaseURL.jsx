import axios from "axios";

// An empty REACT_APP_API_URL means same-origin. The hosted build sets it that
// way: nginx serves this bundle and reverse-proxies the API paths, so no
// backend host is baked in. Unset falls back to a local API for development.
//
// The check is against undefined rather than a falsy test, because an empty
// string is a meaningful value here and `||` would discard it.
const configuredApiUrl = process.env.REACT_APP_API_URL;

export const apiClient = axios.create(
  {
    baseURL: configuredApiUrl === undefined ? 'http://localhost:8080' : configuredApiUrl
  }
)
