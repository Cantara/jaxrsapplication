# jaxrsapplication
Application framework based on Servlets, Jetty, JaxRsWs, Jersey

# Dropwizard Metrics

Documentation: https://metrics.dropwizard.io/4.2.0/index.html

## Cpu Profiler

### Prerequisites

#### MacOS
1. Install `pprof` command: `brew install gperftools`
2. Install `ps2pdf` command: `brew install ghostscript`

#### Amazon Linux AMI
TODO

### Creating a performance profile pdf report
1. Get performance profile data: `curl "http://localhost:8362/admin/pprof?duration=10" > prof`
2. Create pdf from profile data: `pprof --pdf prof > profile.pdf`
