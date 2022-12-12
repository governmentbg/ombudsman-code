<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $Fq_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $Fq_name
 * @property Date   $Fq_date
 */
class MFaq extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_faq';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Fq_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Fq_name', 'Fq_type', 'Fq_order', 'Fq_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Fq_id' => 'int', 'Fq_name' => 'string', 'Fq_type' => 'int', 'Fq_order' => 'int', 'Fq_date' => 'date', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Fq_date', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });


        self::created(function ($article) {
            $truncated = substr($article->Fq_name, 0, 80);
            MFaqLng::create([
                'Fq_id' => $article->Fq_id,
                'S_Lng_id' => 1,
                'FqL_path' =>  Str::slug(C2L($truncated)),
                'FqL_title' => $article->Fq_name,
            ]);
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_faq()
    {
        return $this->hasMany(MFaqLng::class, 'Fq_id');
    }
}
