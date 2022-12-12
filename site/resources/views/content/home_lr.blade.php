@extends('layouts.default')
@section('title', trans('common.title'))
@section('content')
    <div class="m-home">
        {{-- news start --}}
        @php
            $data = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), 1, 0);
            
        @endphp





        {{-- accent end --}}

        {{-- info start --}}
        <div class="row m-info">
            <div class="col-sm-12 col-12 col-lg-12 col-md-12 pl-0">

                <h2 class="m-home-title">{{ trans('common.news') }}</h2>

                <div class="row m-container-front ml-0">
                    @php
                        $cnt = \App\Http\Controllers\MNewsController::countNews(App::getLocale());
                        // dd($cnt);
                    @endphp
                    @if ($cnt)

                        @php
                            $newsLine = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), 4, 0);
                            // dd($newsLine);
                        @endphp

                        @foreach ($newsLine as $news)
                            <div class="col-sm-12 col-lg-6 col-md-6 m-panel m-panel-pl">
                                <a href="/lr/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                    <div class="m-image-f"
                                        style="background: url('/storage{{ $news['media']->ArG_file }}')">
                                    </div>
                                </a>

                                <div class="m-title">
                                    <a href="/lr/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                        {{ $news['MnL_title'] }}
                                        <div class="ar-date">
                                            {{ Carbon\Carbon::createFromDate($news['Mn_date'])->format('d/m/Y') }}</div>
                                    </a>
                                </div>
                            </div>
                        @endforeach
                    @endif
                </div>

                {{-- @php
    $data = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), 2);

@endphp

{{$data}} --}}


                @include('content.position_box')

            </div>




        </div>
    </div>


@stop
