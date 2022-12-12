<ul class="site-menu main-menu js-clone-nav d-lg-block">

    {{-- <li><a href="/{{App::getLocale()}}" class="nav-link">{{ trans('common.home') }}</a></li> --}}
    @foreach (\App\Http\Controllers\NavigationController::navList(App::getLocale(), 1) as $nav)
        @if ($nav->subCount > 0)
            <li class="has-children"><a href="/lr/{{ App::getLocale() }}/p/{{ $nav->ArL_path }}"
                    class="nav-link">{{ $nav->ArL_title }}</a>

                <ul class="dropdown">

                    @foreach ($nav->sub_nav as $nav1)
                        @if ($nav1->subCount > 0)
                            <li class="has-children">
                                <a
                                    href="/lr/{{ App::getLocale() }}/p/{{ $nav1->ArL_path }}">{{ $nav1->ArL_title }}</a>

                                <ul class="dropdown">

                                    @foreach ($nav1->sub_nav as $nav2)
                                        <li>
                                            <a
                                                href="/lr/{{ App::getLocale() }}/p/{{ $nav2->ArL_path }}">{{ $nav2->ArL_title }}</a>
                                        </li>
                                    @endforeach
                                </ul>

                            </li>
                        @else
                            <li>
                                <a
                                    href="/lr/{{ App::getLocale() }}/p/{{ $nav1->ArL_path }}">{{ $nav1->ArL_title }}</a>
                            </li>
                        @endif
                    @endforeach

                </ul>

            </li>
        @else
            <li><a href="/lr/{{ App::getLocale() }}/p/{{ $nav->ArL_path }}"
                    class="nav-link">{{ $nav->ArL_title }}</a></li>
        @endif

        {{-- @foreach

                       @endforeach --}}
    @endforeach





</ul>
