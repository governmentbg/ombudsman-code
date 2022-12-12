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
        <div class="container py-3">
            <div class="row align-items-center">
                <div class="col-sm-12 col-xs-12 col-lg-6 col-md-6 col-xl-6">
                    <div class="d-flex mr-auto m-header">
                        <a href="/lr/{{ App::getLocale() }}">
                            <img src="/img/logo.png" alt="{{ trans('common.title') }}" title="{{ trans('common.title') }}" aria-label="{{ trans('common.title') }}" class="img-fluid m-logo-lr" />
                        </a>
                        <h1 class="m-hd-title">{{ trans('common.title') }}</h1>

                    </div>
                </div>
                <div class="col-sm-12 col-xs-12 col-lg-6 col-md-6 col-xl-6 text-right">

                    <div class="row  text-right">

                        @foreach (\App\Http\Controllers\NavigationController::navList(App::getLocale(), 2) as $nav)
                        <span class="nav-link-head"> <a href="/lr/{{ App::getLocale() }}/p/{{ $nav->ArL_path }}">{{ $nav->ArL_title }}</a>
                            |
                        </span>
                        @endforeach



                        <div class="nav-link-head text-right m-lb">

                            @foreach (\App\Http\Controllers\CommonController::getLng(App::getLocale()) as $l)
                            {{-- {{ dd($l) }} --}}
                            <span class="m-18n">
                                <a href="{{ url('/') }}{{ currentPath(Request::path(), $l->S_Lng_key) }} " aria-label="{{ $l->S_Lng_name }}">
                                    {{ $l->S_Lng_key }}
                                </a>

                            </span>
                            @endforeach
                        </div>

                    </div>
                    <div class="row mt-4 search-group d-low1">
                        <div class="col-xs-12 col-sm-12  col-lg-6 col-md-10 col-xl-6 mb-2 pl-1">
                            <form method="get" action="{{ url('/lr/' . App::getLocale(), 'search') }}">
                                {{-- @csrf --}}
                                <div class="input-group">
                                    <input type="text" class="form-control" placeholder="{{ trans('common.search_label') }}" name="key" aria-label="{{ trans('common.search_label') }}">
                                    <div class="input-group-append">
                                        <button class="btn btn-outline-secondary" type="submit">
                                            <i class="cis-search"></i>
                                        </button>
                                    </div>
                                </div>
                            </form>
                        </div>
                    </div>
                    <div class="row">

                        <div class="col-sm-12  col-12 col-lg-12 col-md-12 col-xl-12 mt-3 text-left hb-1">

                            <span class="claim mr-2">
                                <a href="/lr/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 49) }}">
                                    {{ trans('common.submit_claim') }}</a>
                            </span>
                            <span class="claim">
                                <a href="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}">
                                    {{ trans('common.submit_claim_c') }}</a>
                            </span>

                        </div>

                        <div class="col-sm-12  col-12 col-lg-12 col-md-12 col-xl-12 mt-3 text-left hb-1 lr1">
                            <a href="/{{ App::getLocale() }}">
                                <i class="cis-home"></i>
                                {{ trans('common.main_lr') }}</a>

                        </div>

                    </div>




                </div>
                <div class="row collapse mt-3 pl-5" id="mSearch">
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
                </div>
            </div>
        </div>
    </div>




    @php
    // $data = \App\Http\Controllers\NavigationController::navList(App::getLocale());
    @endphp

    {{-- {{$data}} --}}

    {{-- </div> --}}