<div class="site-mobile-menu site-navbar-target">
    <div class="site-mobile-menu-header">
        <div class="site-mobile-menu-close mt-3">
            <span class="icon-close2 js-menu-toggle"></span>
        </div>
    </div>
    <div class="site-mobile-menu-body"></div>
</div>
<!-- .site-mobile-menu -->


<div class="site-navbar-wrap">
    <div class="site-navbar-top">
        @if (Request::segment(1) != 'prava-na-deteto')
            <div class="m-h-1">
                <div class="container ">
                    <div class="clearfix"></div>

                    {{-- <div class="row">
                        <div class="col-xl-4 col-sm-6 col-xs-6 col-lg-4 col-md-5">
                            lkjljk

                        </div>
                        <div class="col-xl-6 col-sm-6 col-xs-6 col-lg-6 col-md-7">
                            hkhj

                        </div>
                    </div> --}}


                    <div class="row">
                        <div class="col-xl-6 col-lg-6 col-md-6 col-sm-12 col-xs-12 d-low">

                        </div>
                        <div class="col-xl-6 col-lg-6 col-md-6 col-sm-12 col-xs-12 top-b1">
                            <div class="row">
                                <div class="col-xl-7 col-lg-7 col-md-7 col-sm-12 col-xs-12 pl-0 top-b  mb-2">
                                    <div class=" lr1 nav-link-h">
                                        @if (Request::segment(1) == 'lr')
                                            <a href="/{{ App::getLocale() }}">
                                                <i class="cis-home"></i>
                                                {{ trans('common.main_lr') }}</a>
                                        @else
                                            <a href="/lr/{{ App::getLocale() }}">
                                                <i class="cis-eye active-link"></i>
                                                {{ trans('common.low_lr') }}</a>
                                        @endif
                                    </div>
                                </div>
                                <div class="col-xl-3 col-lg-3 col-md-3 col-sm-6 col-xs-6 f-50">
                                    <div class="fr">
                                        <span class="m-font">
                                            {{-- {{ trans('common.font_size') }} --}}
                                            <a class="decrease icon">
                                                - A</a>
                                            {{-- <a class="reset icon">
                                                    A
                                                </a> --}}
                                            <a class="increase icon"> A +
                                            </a>
                                        </span>
                                        <span class="m-search">


                                            <a class="btn m-collapse" data-toggle="collapse" href="#searchSite"
                                                role="button" aria-expanded="false" aria-controls="searchSite">
                                                <i class="cis-search"></i> </a>
                                        </span>
                                    </div>

                                </div>
                                <div class="col-xl-2 col-lg-2 col-md-2 col-sm-6 col-xs-6 f-50 ">
                                    <div class="i18n">
                                        @foreach (\App\Http\Controllers\CommonController::getLng(App::getLocale()) as $l)
                                            <span class="m-18n">
                                                <a href="{{ url('/') }}{{ currentPath(Request::path(), $l->S_Lng_key) }}
"
                                                    aria-label="{{ $l->S_Lng_name }}">
                                                    {{ $l->S_Lng_name }}
                                                </a>

                                            </span>
                                        @endforeach
                                    </div>
                                </div>


                            </div>
                            <div class="row collapse multi-collapse" id="searchSite">

                                <div class="col-sm-12  col-12 col-lg-12 col-md-12 mb-2 ">
                                    <form method="get" action="{{ url(App::getLocale(), 'site-search') }}">
                                        {{-- @csrf --}}
                                        <div class="input-group">
                                            <input type="text" class="form-control"
                                                placeholder="{{ trans('common.search_string') }}" name="key"
                                                aria-label="{{ trans('common.search_label') }}">
                                            <div class="input-group-append">
                                                <button class="btn btn-outline-secondary" type="submit">
                                                    <i class="cis-search"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </form>
                                </div>

                            </div>

                        </div>
                    </div>





                </div>
            </div>
        @endif
        <div class="m-h-2">
            <div class="container py-3">
                <div class="row align-items-center">
                    <div class="col-xl-6 col-lg-6 col-md-6 col-sm-12 col-xs-12">
                        <div class="d-flex mr-auto m-header">
                            <a href="/{{ App::getLocale() }}">
                                <img src="/img/logo_horizontal_{{ App::getLocale() }}.svg"
                                    alt="{{ trans('common.title') }}" title="{{ trans('common.title') }}"
                                    aria-label="{{ trans('common.title') }}" class="img-fluid m-logo" />
                            </a>
                            {{-- <h1 class="m-hd-title">{{ trans('common.title') }}</h1> --}}

                        </div>
                    </div>

                    @if (Request::segment(1) == 'prava-na-deteto')
                        <div class="col-xl-6 col-lg-6 col-md-6 col-sm-12 col-xs-12 text-center pt-3 d-low">
                            <span class="m-cr-claim-btn">
                                <a
                                    href="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}">
                                    {{-- {{ trans('common.submit_claim_c') }} --}}
                                    {{ trans('common.claim.signal') }}
                                </a>
                            </span>
                        </div>
                    @endif

                    @if (Request::segment(1) != 'prava-na-deteto')
                        <div class="col-xl-6 col-lg-6 col-md-6 col-sm-12 col-xs-12 text-right">


                            <div class="row mt-4 search-group d-low">
                                <div class="col-sm-12  col-12 col-lg-12 col-md-12 mb-2 pl-0">
                                    <form method="get" action="{{ url(App::getLocale(), 'site-search') }}">
                                        {{-- @csrf --}}
                                        <div class="input-group">
                                            <input type="text" class="form-control"
                                                placeholder="{{ trans('common.search_string') }}" name="key"
                                                aria-label="{{ trans('common.search_label') }}">
                                            <div class="input-group-append">
                                                <button class="btn btn-outline-secondary" type="submit">
                                                    <i class="cis-search"></i>
                                                </button>
                                            </div>
                                        </div>
                                    </form>
                                </div>







                            </div>




                        </div>
                        {{-- <div class="row collapse mt-3 pl-5" id="mSearch">
                            <div class="col search-group">
                                <form method="get" action="{{ url(App::getLocale(), 'search') }}">
                    <div class="input-group ">
                        <input type="text" class="form-control" name="key" placeholder="Търсене" aria-label="Търсене">
                        <div class="input-group-append">
                            <button class="btn btn-outline-secondary" type="submit"> <i class="cis-search"></i>
                            </button>
                        </div>
                    </div>
                    </form>
                </div>
            </div> --}}
                    @endif
                </div>
            </div>
        </div>

    </div>

    <div class="site-navbar site-navbar-target js-sticky-header m-navbar">
        <div class="container">
            <div class="row align-items-center">
                {{-- <div class="col-2 d-none">
              <h1 class="my-0 site-logo"><a href="index.html">Logo</a></h1>
            </div> --}}
                <div class="col-12  m-nav-r">
                    <nav class="site-navigation text-right1 m-navigation" role="navigation">
                        <div class="container pl-0">

                            <div class="d-inline-block d-lg-none ml-md-0 mr-auto py-3">
                                <div class="row">
                                    <div class="col-12">
                                        <a href="#" class="site-menu-toggle js-menu-toggle"><span
                                                class="icon-menu h3"></span>
                                        </a>

                                        @if (Request::segment(1) != 'lr' && Request::segment(1) != 'prava-na-deteto')
                                            <div class="float-right mt-1">

                                                <a class="claim-m"
                                                    href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 49) }}">
                                                    {{ trans('common.submit_claim') }}</a>

                                                <a class="claim-m"
                                                    href="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}">
                                                    {{ trans('common.submit_claim_c') }}</a>

                                            </div>
                                        @endif
                                        @if (Request::segment(1) == 'prava-na-deteto')
                                            <div class="float-right mr-5 mt-2">

                                                <span class="m-cr-claim-btn">
                                                    <a
                                                        href="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}">
                                                        {{-- {{ trans('common.submit_claim_c') }} --}}
                                                        {{ trans('common.claim.signal') }}
                                                    </a>
                                                </span>

                                            </div>
                                        @endif

                                    </div>


                                    {{-- @if (Request::segment(1) != 'lr' && Request::segment(1) != 'prava-na-deteto')
                                        <div class="col-10 pt-1 text-right m-menu-tg">
                                           
                                            <span class="collapse" id="addClaim">
                                                <a href="/{{ App::getLocale() }}/p/poday-zhalba">
                                {{ trans('common.submit_claim') }}
                                </a>
                                </span>
                                <i class="cis-folder-shared tg py-3" data-toggle="collapse" data-target="#addClaim" aria-expanded="false" aria-controls="addClaim"></i>

                                <i class="cis-search tg" data-toggle="collapse" data-target="#mSearch" aria-expanded="false" aria-controls="mSearch"></i>

                            </div>
                            <div class="col-4">

                            </div>
                            @endif --}}
                                    {{-- <div class="col-sm-6">
                                    dd
                                </div>
                                <div class="col-sm-4">
                                    ee
                                </div> --}}
                                </div>




                            </div>

                            @php
                                if (Request::segment(1) == 'prava-na-deteto') {
                                    $navType = 3;
                                } else {
                                    $navType = 1;
                                }
                            @endphp

                            <ul class="site-menu main-menu js-clone-nav d-none d-lg-block p-0">

                                @if (Request::segment(1) == 'prava-na-deteto')
                                    <li class="d-low">


                                        <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/tvoite-prava-kato-dete"
                                            class="nav-link">
                                            <i class="cil-home"></i>
                                        </a>

                                    </li>
                                @endif

                                {{-- <li><a href="/{{App::getLocale()}}" class="nav-link">{{ trans('common.home') }}</a></li> --}}
                                @foreach (\App\Http\Controllers\NavigationController::navList(App::getLocale(), $navType) as $nav)
                                    @if ($nav->subCount > 0)
                                        <li class="has-children"><a
                                                href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $nav->ArL_path }}"
                                                class="nav-link">{{ $nav->ArL_title }}</a>

                                            <ul class="dropdown">

                                                @foreach ($nav->sub_nav as $nav1)
                                                    @if ($nav1->subCount > 0)
                                                        <li class="has-children">
                                                            <a
                                                                href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $nav1->ArL_path }}">{{ $nav1->ArL_title }}</a>

                                                            <ul class="dropdown">

                                                                @foreach ($nav1->sub_nav as $nav2)
                                                                    <li>
                                                                        <a
                                                                            href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $nav2->ArL_path }}">{{ $nav2->ArL_title }}</a>
                                                                    </li>
                                                                @endforeach
                                                            </ul>

                                                        </li>
                                                    @else
                                                        <li>
                                                            <a
                                                                href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $nav1->ArL_path }}">{{ $nav1->ArL_title }}</a>
                                                        </li>
                                                    @endif
                                                @endforeach

                                            </ul>

                                        </li>
                                    @else
                                        <li>
                                            {{-- /prava-na-deteto/bg/p/tvoite-prava-kato-dete --}}
                                            @if ($nav->ArL_url)
                                                <a href="{{ $nav->ArL_url }}" class="nav-link">
                                                @else
                                                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $nav->ArL_path }}"
                                                        class="nav-link">
                                            @endif
                                            {{-- {!! $nav->Ar_icon !!} --}}
                                            {{ $nav->ArL_title }}</a>

                                        </li>
                                    @endif

                                    {{-- @foreach

                       @endforeach --}}
                                @endforeach





                            </ul>
                        </div>
                    </nav>
                </div>
            </div>
        </div>


    </div>



    @php
        // $data = \App\Http\Controllers\NavigationController::navList(App::getLocale());
    @endphp

    {{-- {{$data}} --}}

    {{-- </div> --}}
