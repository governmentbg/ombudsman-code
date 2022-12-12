<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use KossShtukert\LaravelNovaSelect2\Select2;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Http\Requests\NovaRequest;

class ULevel2article extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\ULevel2article::class;
    public static $displayInNavigation = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'Ula_id';
    public static function label()
    {
        return 'Допълнително управление на секции';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'Ula_id',
    ];

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {

        $nav = DB::table('m_article')
            ->select('Ar_name as name', 'Ar_id as id')
            ->get()->pluck('name', 'id');

        return [
            ID::make(__('ID'), 'Ula_id')->sortable(),
            Select2::make('Секция', 'Ar_id')
                ->sortable()
                ->required()
                ->options($nav)
                ->displayUsingLabels()
                ->showAsLink()
                ->onlyOnForms()
                ->configuration([
                    'placeholder'             => __('Choose an option'),
                    'allowClear'              => true,
                    'minimumResultsForSearch' => 1,
                    'multiple'                => false,
                ]),

            BelongsTo::make(
                'Секция',
                'ul_articles',
                MArticle::class
            )->rules('required')->withoutTrashed()->hideWhenCreating()->hideWhenUpdating(),
            BelongsTo::make('Ниво на достъп', 'ul_levels', ULevels::class)->rules('required')->withoutTrashed(),
        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}
