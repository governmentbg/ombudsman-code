<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">

<title>@yield('title')</title>
<meta name="p:domain_verify" content="" />
<meta charset="UTF-8">
<meta name="description" content="@yield('site-description')" />
<meta name="site-keywords" content="@yield('site-description')" />
<meta name="author" content="ombudsman.bg, 2010 - <?= date('Y') ?>" />
<meta name="robots" content="index, follow" />
<meta name="googlebot" content="index, follow" />
<meta name="revisit-after" content="10 days" />
<meta name="revisit" content="10 days" />
{{-- <!-- Twitter Card data -->
<meta name="twitter:card" content="">
<meta name="twitter:site" content="">
<meta name="twitter:title" content="">
<meta name="twitter:description" content="">
<meta name="twitter:creator" content="">
<!-- Twitter summary card -->
<meta name="twitter:image:src" content="{{ url('/') }}@yield('article-image')"> --}}
<!-- Open Graph data -->
<meta property="og:title" content="@yield('article-headline')" />
<meta property="og:type" content="article" />
<meta property="og:url" content="{{ url('/') }}/{{ App::getLocale() }}@yield('site-canonical')" />
<meta property="og:image" content="{{ url('/') }}/{{ App::getLocale() }}@yield('article-image')" />
<meta property="og:description" content="@yield('article-description')" />
<meta property="og:site_name" content="@yield('title')" />
<meta property="article:published_time" content="@yield('article-date')" />
<meta property="article:modified_time" content="@yield('article-modification-date')" />
<meta property="article:tag" content="@yield('article-tag')" />
<link rel="canonical" href="{{ url('/') }}/{{ App::getLocale() }}@yield('site-canonical')" />


<link rel="stylesheet" href="/css/datepicker.min.css">
<link href="{{ mix('css/app.css') }}" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="/fonts/icomoon/style.css">

<link href="/css/all.min.css" type="text/css" rel="stylesheet" />
<link href="/css/owl.carousel.min.css" type="text/css" rel="stylesheet" />
<link href="/css/bootstrap.min.css" type="text/css" rel="stylesheet" />
<link rel="stylesheet" href="/css/ekko-lightbox.css">
<link rel="stylesheet" href="/css/select2.min.css">
<link rel="stylesheet" href="/css/video-js.css">
<link rel="stylesheet" href="/css/perfect-scrollbar.css">
<x-embed-styles />
